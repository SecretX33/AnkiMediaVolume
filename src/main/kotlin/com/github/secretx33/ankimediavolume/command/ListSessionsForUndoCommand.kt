package com.github.secretx33.ankimediavolume.command

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.secretx33.ankimediavolume.model.FileAttributesInfo
import com.github.secretx33.ankimediavolume.model.RenameSession
import com.github.secretx33.ankimediavolume.model.RenamedFile
import com.github.secretx33.ankimediavolume.util.moveToTrash
import com.github.secretx33.ankimediavolume.util.objectMapper
import com.github.secretx33.ankimediavolume.util.readInt
import com.github.secretx33.ankimediavolume.util.setModifiedTimes
import com.github.secretx33.ankimediavolume.util.shiftBy
import org.slf4j.LoggerFactory
import toothpick.InjectConstructor
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Singleton
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.readText

@Singleton
@InjectConstructor
class ListSessionsForUndoCommand : ExecutionCommand {

    override val name: String = "Undo rename sessions"

    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun CommandContext.execute() {
        val undoFolder = configuration.undoSessionsFolderPath
        val sessions = undoFolder.takeIf { it.exists() }?.listDirectoryEntries("*.json")
            ?.filter { it.isRegularFile() }.orEmpty()
            .ifEmpty {
                log.info("No rename sessions found to undo.")
                scanner.nextLine()
                return
            }.sortRenameSessionPaths()

        println("""
            Select a session to undo:
        
            ${sessions.withIndex().joinToString("\n") { (index, value) -> 
                "${index + 1}. ${value.name}" 
            }}
        """.trimIndent())

        val selectedSession = sessions[scanner.readInt(sessions.indices.shiftBy(1)) - 1]
        val renameSession = try {
            objectMapper.readValue<RenameSession>(selectedSession.readText())
        } catch (e: Exception) {
            log.error("Error while reading rename session file '${selectedSession.absolutePathString()}'", e)
            scanner.nextLine()
            return
        }

        log.info("""
            Rename session info:
               File: ${selectedSession.absolutePathString()}
               Anki media folder: ${renameSession.ankiMediaFolderPath.absolutePathString()}
               Date: ${renameSession.date.format(DateTimeFormatter.ISO_DATE_TIME)}
        """.trimIndent() + "\n")

        val renamedFiles = renameSession.files
        val results = renamedFiles.withIndex().associateWith { (_, file) ->
            renameFile(file, renameSession)
        }.onEach { (indexedFile, result) ->
            val (index, file) = indexedFile
            logRenameStatus(index, file, result, renamedFiles)
        }

        if (results.any { it.value != RenameResult.SUCCESSFULLY_RENAMED }) {
            val successCount = results.count { it.value == RenameResult.SUCCESSFULLY_RENAMED }
            log.info("\nRename session '${selectedSession.name}' was partially undone, $successCount out of ${renamedFiles.size} files were successfully unrenamed, session file '${selectedSession.name}' and lock file were NOT deleted.\nPlease double-check the error messages above and take the appropriate action.")
            scanner.nextLine()
            return
        }

        selectedSession.moveToTrash()
        configuration.ankiMediaLockFile.deleteIfExists()
        log.info("\nRename session '${selectedSession.name}' undone successfully, all files were unrenamed, session file '${selectedSession.name}' and lock file were deleted.")
        scanner.nextLine()
    }

    private fun renameFile(
        file: RenamedFile,
        renameSession: RenameSession,
    ): RenameResult {
        val renamedFile = renameSession.ankiMediaFolderPath / file.renamedName
        val originalFile = renameSession.ankiMediaFolderPath / file.originalName

        return when {
            renamedFile.notExists() -> RenameResult.RENAMED_FILE_NOT_FOUND
            originalFile.exists() -> RenameResult.ORIGINAL_FILE_ALREADY_EXISTS
            else -> RenameResult.SUCCESSFULLY_RENAMED.also {
                renamedFile.moveTo(originalFile)
                originalFile.restoreTimes(file.fileAttributesInfo)
            }
        }
    }

    private fun Path.restoreTimes(fileAttribute: FileAttributesInfo) = setModifiedTimes(
        lastModifiedAt = fileAttribute.lastModifiedAt,
        createdAt = fileAttribute.createdAt,
    )

    private fun logRenameStatus(
        index: Int,
        file: RenamedFile,
        result: RenameResult,
        renamedFiles: Collection<RenamedFile>,
    ) {
        val prefix = "${index + 1}/${renamedFiles.size}."

        when (result) {
            RenameResult.ORIGINAL_FILE_ALREADY_EXISTS -> log.warn("$prefix File '${file.renamedName}' cannot be renamed back to '${file.originalName}' because file '${file.originalName}' already exists in Anki media folder, skipping it...")
            RenameResult.RENAMED_FILE_NOT_FOUND -> log.warn("$prefix File '${file.renamedName}' (originally '${file.originalName}') was not found in Anki media folder, skipping it...")
            RenameResult.SUCCESSFULLY_RENAMED -> log.info("$prefix Renamed '${file.renamedName}' -> '${file.originalName}'")
        }
    }

    private enum class RenameResult {
        ORIGINAL_FILE_ALREADY_EXISTS,
        RENAMED_FILE_NOT_FOUND,
        SUCCESSFULLY_RENAMED,
    }

    private fun Iterable<Path>.sortRenameSessionPaths(): List<Path> = mapNotNullTo(mutableSetOf()) {
        val dateString = RENAME_SESSION_NAME_REGEX.matchEntire(it.name)?.groupValues?.get(1)
            ?: return@mapNotNullTo null
        try {
            LocalDateTime.parse(dateString, RENAME_SESSION_DATE_FORMAT) to it
        } catch (e: DateTimeParseException) {
            null
        }
    }.sortedBy { it.first }
        .map { it.second }

    companion object {
        const val RENAME_SESSION_NAME_TEMPLATE = "rename_session_{date}.json"
        val RENAME_SESSION_NAME_REGEX = "^rename_session_(.*)\\.json$".toRegex()
        val RENAME_SESSION_DATE_FORMAT: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss") }
    }
}