package com.github.secretx33.ankimediavolume.command

import com.github.secretx33.ankimediavolume.command.ListSessionsForUndoCommand.Companion.RENAME_SESSION_DATE_FORMAT
import com.github.secretx33.ankimediavolume.command.ListSessionsForUndoCommand.Companion.RENAME_SESSION_NAME_TEMPLATE
import com.github.secretx33.ankimediavolume.model.FileAttributesInfo
import com.github.secretx33.ankimediavolume.model.RenameSession
import com.github.secretx33.ankimediavolume.model.RenamedFile
import com.github.secretx33.ankimediavolume.util.createFileIfNotExists
import com.github.secretx33.ankimediavolume.util.prettyObjectMapper
import com.github.secretx33.ankimediavolume.util.readOption
import org.slf4j.LoggerFactory
import toothpick.InjectConstructor
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
        // Detects if media folder is already renamed, then promptly refuses to continue
        if (configuration.ankiMediaLockFile.exists()) {
            log.info("Anki media folder is already renamed, please undo the previous rename session before continuing. If you believe this is an error, please manually delete the lock file at '${configuration.ankiMediaLockFile.absolutePathString()}', then try again.")
            scanner.nextLine()
            return
        }

        // Asks user for confirmation
        if (!scanner.askForRenameConfirmation()) return

        // Find which media files need to be renamed, then generate a new random UUID name for them
        val mediaFiles = configuration.ankiMediaFolderPath.listDirectoryEntries("*.mp3")
            .filter { NOT_ASCII_REGEX in it.name }
        val renamedFiles = mediaFiles.mapTo(mutableSetOf()) { it.toRenamedFile() }

        // Save the new names in a JSON file within the programs folder, so we can rollback later
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        createRenameSession(now, renamedFiles)

        // Creates a lock file to indicate that the media folder is already renamed
        configuration.ankiMediaLockFile.createFileIfNotExists()

        // Renames media folder files
        renamedFiles.forEachIndexed { index, renamedFile ->
            val originalFile = configuration.ankiMediaFolderPath / renamedFile.originalName
            val renamedFile = configuration.ankiMediaFolderPath / renamedFile.renamedName
            try {
                originalFile.moveTo(renamedFile)
            } catch (e: Exception) {
                log.error("Error while renaming file '${originalFile.absolutePathString()}' to '${renamedFile.absolutePathString()}'", e)
                scanner.nextLine()
                return
            }
            log.info("${index + 1}/${renamedFiles.size}: Renamed '${originalFile.absolutePathString()}' -> '${renamedFile.absolutePathString()}'")
        }

        log.info("All files renamed successfully, created new rename session '${getRenameSessionFileName(now)}' with the information to allow its undo.")
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
    ) {
        val session = RenameSession(
            date = now,
            ankiMediaFolderPath = configuration.ankiMediaFolderPath,
            files = renamedFiles,
        )
        val sessionFile = configuration.rollbackFolderPath / getRenameSessionFileName(now)
        sessionFile.createFileIfNotExists().writeText(prettyObjectMapper.writeValueAsString(session))
    }

    private fun getRenameSessionFileName(now: OffsetDateTime): String =
        "${RENAME_SESSION_NAME_TEMPLATE.replace("{date}", now.format(RENAME_SESSION_DATE_FORMAT))}.json"

    private companion object {
        val NOT_ASCII_REGEX = """\p{^ASCII}""".toRegex(RegexOption.IGNORE_CASE)
    }

}