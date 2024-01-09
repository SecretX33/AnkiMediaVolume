package com.github.secretx33.ankimediavolume.command

import com.github.secretx33.ankimediavolume.command.ListSessionsForUndoCommand.Companion.RENAME_SESSION_DATE_FORMAT
import com.github.secretx33.ankimediavolume.command.ListSessionsForUndoCommand.Companion.RENAME_SESSION_NAME_TEMPLATE
import com.github.secretx33.ankimediavolume.model.FileAttributesInfo
import com.github.secretx33.ankimediavolume.model.RenameSession
import com.github.secretx33.ankimediavolume.model.RenamedFile
import com.github.secretx33.ankimediavolume.util.createFileIfNotExists
import com.github.secretx33.ankimediavolume.util.prettyObjectMapper
import com.github.secretx33.ankimediavolume.util.readOption
import com.github.secretx33.ankimediavolume.util.setModifiedTimes
import org.slf4j.LoggerFactory
import toothpick.InjectConstructor
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Scanner
import java.util.UUID
import javax.inject.Singleton
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.readAttributes
import kotlin.io.path.writeText

@Singleton
@InjectConstructor
class RenameMediaFilesCommand : ExecutionCommand {

    override val name: String = "Rename media files"

    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun CommandContext.execute() {
        if (configuration.ankiMediaLockFile.exists()) {
            log.info("Anki media folder is already renamed, please undo the previous rename session before continuing.\n\nIf you believe this is an error, please manually delete the lock file at '${configuration.ankiMediaLockFile}', then try again.")
            scanner.nextLine()
            return
        }

        if (configuration.ankiMediaFolderPath.notExists()) {
            log.error("Anki media folder does not exist on configured path '${configuration.ankiMediaFolderPath}'.\n\nPlease make sure you have Anki installed, that you have opened it at least once, and that the configured Anki media folder actually exists, then try again.")
            scanner.nextLine()
            return
        }

        if (!scanner.askForRenameConfirmation()) return

        val mediaFiles = configuration.ankiMediaFolderPath.listDirectoryEntries("*.mp3")
            .filter { NOT_ASCII_REGEX in it.name }
            .ifEmpty {
                log.info("No media files found to rename.")
                scanner.nextLine()
                return
            }
        val renamedFiles = mediaFiles.mapTo(mutableSetOf()) { it.toRenamedFile() }

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val renameSession = createRenameSession(now, renamedFiles)

        log.info("\n"+ """
            Rename session info:
               Anki media folder: ${renameSession.ankiMediaFolderPath.absolutePathString()}
               Date: ${renameSession.date.format(DateTimeFormatter.ISO_DATE_TIME)}
        """.trimIndent() + "\n")

        // Creates a lock file to indicate that the media folder is already renamed
        configuration.ankiMediaLockFile.createFileIfNotExists()

        log.info("Found ${renamedFiles.size} files to rename. Starting rename process...\n")

        renamedFiles.forEachIndexed { index, file ->
            val originalFile = configuration.ankiMediaFolderPath / file.originalName
            val renamedFile = configuration.ankiMediaFolderPath / file.renamedName
            try {
                originalFile.moveTo(renamedFile)
            } catch (e: Exception) {
                log.error("Error while renaming file '${originalFile.absolutePathString()}' to '${renamedFile.absolutePathString()}'", e)
                scanner.nextLine()
                return
            }
            log.info("${index + 1}/${renamedFiles.size}. Renamed '${file.originalName}' -> '${file.renamedName}'")
        }

        log.info("\nAll ${renamedFiles.size} files renamed successfully, created new rename session '${getRenameSessionFileName(now)}' with the information to allow its undo.")
        scanner.nextLine()
    }

    /**
     * Asks for confirmation of the user if he wants to proceed with the renaming operation or not.
     *
     * Returns `true` if user wants to continue, `false` otherwise.
     */
    private fun Scanner.askForRenameConfirmation(): Boolean {
        log.info("""
            This will rename all media files in the Anki media folder, and will also create a rollback session file in the program folder.
            If you want to undo the rename later, you can run the program again and select the undo option.
            Do you want to continue? (y/n)
        """.trimIndent())
        val selectedOption = readOption(options = setOf("y", "n"), forceLowercase = true)
        return selectedOption == "y"
    }

    private fun Path.generateNewRandomName(): String = absolute().run {
        generateSequence { parent!! / "${UUID.randomUUID()}.${extension}" }
            .first { it.notExists() }
            .name
    }

    private fun Path.toRenamedFile(): RenamedFile  {
        val attributes = readAttributes<BasicFileAttributes>()
        return RenamedFile(
            originalName = name,
            renamedName = generateNewRandomName(),
            fileAttributesInfo = FileAttributesInfo(
                createdAt = attributes.creationTime().toInstant(),
                lastModifiedAt = attributes.lastModifiedTime().toInstant(),
            )
        )
    }

    private fun CommandContext.createRenameSession(
        now: OffsetDateTime,
        renamedFiles: Set<RenamedFile>,
    ): RenameSession {
        val session = RenameSession(
            date = now,
            ankiMediaFolderPath = configuration.ankiMediaFolderPath,
            files = renamedFiles,
        )
        val sessionFile = configuration.undoSessionsFolderPath / getRenameSessionFileName(now)
        sessionFile.createFileIfNotExists().writeText(prettyObjectMapper.writeValueAsString(session))
        return session
    }

    private fun getRenameSessionFileName(now: OffsetDateTime): String =
        RENAME_SESSION_NAME_TEMPLATE.replace("{date}", now.format(RENAME_SESSION_DATE_FORMAT))

    private companion object {
        val NOT_ASCII_REGEX = """[^\p{ASCII}]""".toRegex(RegexOption.IGNORE_CASE)
    }

}