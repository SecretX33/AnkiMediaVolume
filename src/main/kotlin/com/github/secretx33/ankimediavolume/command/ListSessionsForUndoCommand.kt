package com.github.secretx33.ankimediavolume.command

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.secretx33.ankimediavolume.model.RenameSession
import com.github.secretx33.ankimediavolume.model.RenamedFile
import com.github.secretx33.ankimediavolume.util.moveToTrash
import com.github.secretx33.ankimediavolume.util.objectMapper
import com.github.secretx33.ankimediavolume.util.readInt
import org.slf4j.LoggerFactory
import toothpick.InjectConstructor
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.readText

@InjectConstructor
class ListSessionsForUndoCommand : ExecutionCommand {

    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun CommandContext.execute() {
        val rollbackFolder = configuration.rollbackFolderPath
        val sessions = rollbackFolder.listDirectoryEntries("*.json").filter { it.isRegularFile() }
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

        val selectedSession = sessions[scanner.readInt(sessions.indices)]
        val renameSession = try {
            objectMapper.readValue<RenameSession>(selectedSession.readText())
        } catch (e: Exception) {
            log.error("Error while reading rename session file '${selectedSession.absolutePathString()}'", e)
            scanner.nextLine()
            return
        }

        log.info("""
            Rename session info
              - Date: ${renameSession.date.format(DateTimeFormatter.ISO_DATE_TIME)}
              - Anki media folder: ${renameSession.ankiMediaFolderPath.absolutePathString()}
              - File: ${selectedSession.absolutePathString()}
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
            log.info("\nRename session '${selectedSession.name}' was partially undone, $successCount out of ${renamedFiles.size} files were successfully unrenamed, session file '${selectedSession.name}' was NOT deleted.\nPlease double-check the error messages above and take the appropriate action.")
            scanner.nextLine()
            return
        }

        selectedSession.moveToTrash()
        log.info("\nRename session '${selectedSession.name}' undone successfully, all files were unrenamed, session file '${selectedSession.name}' was deleted.")
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
            }
        }
    }

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

    private companion object {
        val RENAME_SESSION_NAME_REGEX by lazy { "^rename_session_(.*)\\.json$".toRegex() }
        val RENAME_SESSION_DATE_FORMAT: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss") }
    }
}