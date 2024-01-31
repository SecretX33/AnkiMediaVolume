package com.github.secretx33.ankimediavolume.command

import com.github.secretx33.ankimediavolume.model.FileAttributesInfo
import com.github.secretx33.ankimediavolume.model.RenameSession
import com.github.secretx33.ankimediavolume.model.RenamedFile
import com.github.secretx33.ankimediavolume.util.createFileIfNotExists
import com.github.secretx33.ankimediavolume.util.prettyObjectMapper
import toothpick.InjectConstructor
import java.awt.Desktop
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Singleton
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
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
@OptIn(ExperimentalPathApi::class)
class AudioFilesNotNormalizedUsingFolderCommand : AudioFilesNotNormalizedCommand() {

    override val name: String = "Normalize audio files using temporary folder"

    override fun CommandContext.getMediaFiles(): Collection<Path> = configuration.ankiMediaFolderPath
        .listDirectoryEntries("*.mp3")
        .filter { !it.isVolumeNormalized() }

    override fun CommandContext.execute() {
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

        val mediaFiles = getMediaFiles().ifEmpty {
            log.info("No media files found to rename.")
            scanner.nextLine()
            return
        }
        val temporaryAudioFolder = (configuration.temporaryAudiosFolderPath / UUID.randomUUID().toString()).createDirectories()
        val renamedFiles = mediaFiles.mapTo(mutableSetOf()) { it.toRenamedFile() }

        log.info("\n" + """
            Rename session info:
               Anki media folder: ${configuration.ankiMediaFolderPath.absolutePathString()}
               Files: ${renamedFiles.size}
               Date: ${OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)}
               Temporary audio folder: ${temporaryAudioFolder.absolutePathString()}
        """.trimIndent() + "\n")

        renamedFiles.forEachIndexed { index, file ->
            val originalFile = configuration.ankiMediaFolderPath / file.originalName
            val temporaryFile = temporaryAudioFolder / file.renamedName
            try {
                originalFile.copyTo(temporaryFile)
            } catch (e: Exception) {
                log.error("Error while copying file '${originalFile.absolutePathString()}' to '${temporaryFile.absolutePathString()}'", e)
                scanner.nextLine()
                return
            }
            log.info("${index + 1}/${renamedFiles.size}. Copied file '${file.renamedName}' -> '${file.originalName}'")
        }

        Desktop.getDesktop().open(temporaryAudioFolder.toFile())
        log.info("\nOpened temporary audio folder in file explorer, please normalize the volume of the audio files there, close the file explorer, and only then press enter to continue.")
        Thread.sleep(1500)
        scanner.nextLine()

        log.info("\nCopying all files from the temporary folder back to Anki media folder.\n")

        renamedFiles.forEachIndexed { index, file ->
            val originalFile = configuration.ankiMediaFolderPath / file.originalName
            val temporaryFile = temporaryAudioFolder / file.renamedName
            try {
                temporaryFile.moveTo(originalFile, overwrite = true)
            } catch (e: Exception) {
                log.error("Error while moving file '${temporaryFile.absolutePathString()}' to '${originalFile.absolutePathString()}'", e)
                scanner.nextLine()
                return
            }
            log.info("${index + 1}/${renamedFiles.size}. Copied file '${file.renamedName}' -> '${file.originalName}'")
        }
        temporaryAudioFolder.deleteRecursively()

        log.info("\nAll ${renamedFiles.size} files replaced successfully.")
        scanner.nextLine()
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

    companion object {
        const val RENAME_SESSION_NAME_TEMPLATE = "rename_session_{date}.json"
        val RENAME_SESSION_NAME_REGEX by lazy { "^rename_session_(.*)\\.json$".toRegex() }
        val RENAME_SESSION_DATE_FORMAT: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss") }
        val NOT_ASCII_REGEX by lazy { """[^\p{ASCII}]""".toRegex(RegexOption.IGNORE_CASE) }
    }
}