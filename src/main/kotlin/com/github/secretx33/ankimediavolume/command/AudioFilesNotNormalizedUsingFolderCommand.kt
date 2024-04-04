package com.github.secretx33.ankimediavolume.command

import com.github.secretx33.ankimediavolume.model.Configuration
import com.github.secretx33.ankimediavolume.model.FileAttributesInfo
import com.github.secretx33.ankimediavolume.model.NormalizedFile
import com.github.secretx33.ankimediavolume.model.RenamedFile
import com.github.secretx33.ankimediavolume.repository.NormalizedFileRepository
import com.mpatric.mp3agic.Mp3File
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

@Singleton
@InjectConstructor
@OptIn(ExperimentalPathApi::class)
class AudioFilesNotNormalizedUsingFolderCommand(private val fileRepository: NormalizedFileRepository) : ExecutionCommand {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    override val name: String = "Normalize audio files using temporary folder (not normalized only)"

    private fun CommandContext.getMediaFiles(): Collection<Path> {
        val allAudioFiles = configuration.ankiMediaFolderPath
            .listDirectoryEntries("*.mp3")
        val normalizedFileNames = fileRepository.getNormalizedFilesIn(configuration.ankiMediaFolderPath, allAudioFiles.mapTo(mutableSetOf()) { it.name })
        val possibleNotNormalizedFiles = allAudioFiles.filterTo(mutableSetOf()) { it.name !in normalizedFileNames }

        // Add the file that is normalized (but not yet in our normalized file database) to the database
        val normalizedFiles = possibleNotNormalizedFiles.filterTo(mutableSetOf()) { it.isVolumeNormalized() }.takeIf { it.isNotEmpty() }
            ?.also {
                log.info("The following ${it.size} files are already volume normalized and will be ignored:\n\n${it.withIndex().joinToString("\n") { "${it.index}. ${it.value.name}" }}")
                fileRepository.insertAll(it.map { NormalizedFile(configuration.ankiMediaFolderPath, it.name) })
            }.orEmpty()

        return possibleNotNormalizedFiles - normalizedFiles
    }

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
            log.info("${index + 1}/${renamedFiles.size}. Copied file '${file.originalName}' -> '${file.renamedName}'")
        }

        Desktop.getDesktop().open(temporaryAudioFolder.toFile())
        log.info("\nOpened temporary audio folder in file explorer, please normalize the volume of the audio files there, close the file explorer, and only then press enter to continue.")
        Thread.sleep(1500)
        scanner.nextLine()

        log.info("\nCopying all files from the temporary folder back to Anki media folder.\n")

        renamedFiles.forEachIndexed { index, file ->
            val originalFile = buildOriginalFile(configuration, file)
            val temporaryFile = temporaryAudioFolder / file.renamedName
            try {
                temporaryFile.moveTo(originalFile, overwrite = true)
            } catch (e: Exception) {
                log.error("Error while moving file '${temporaryFile.absolutePathString()}' to '${originalFile.absolutePathString()}'", e)
                scanner.nextLine()
                return
            }
            log.info("${index + 1}/${renamedFiles.size}. Moved file '${file.renamedName}' -> '${file.originalName}'")
        }
        temporaryAudioFolder.deleteRecursively()

        val normalizedFiles = renamedFiles.filterTo(mutableSetOf()) {
            val originalFile = buildOriginalFile(configuration, it)
            originalFile.isVolumeNormalized()
        }
        fileRepository.insertAll(normalizedFiles.map { NormalizedFile(configuration.ankiMediaFolderPath, it.originalName) })

        val nonNormalizedFiles = renamedFiles - normalizedFiles
        when {
            nonNormalizedFiles.isNotEmpty() -> {
                log.error("\n\nWhile replacing files, found out that ${nonNormalizedFiles.size} out of ${renamedFiles.size} were not normalized, did you forget to normalize these files?\n\n${nonNormalizedFiles.joinToString("\n") { "- ${it.originalName}" }}\n\nPress any button to continue.")
                Thread.sleep(1000)
            }
            else -> log.info("\nAll ${renamedFiles.size} files replaced successfully.")
        }

        scanner.nextLine()
    }

    private fun buildOriginalFile(configuration: Configuration, renamedFile: RenamedFile): Path =
        configuration.ankiMediaFolderPath / renamedFile.originalName

    private fun Path.isVolumeNormalized(): Boolean {
        val mp3 = try {
            Mp3File(toFile())
        } catch (e: Exception) {
            log.debug("File '$this' could not be parsed, assume it is not normalized", e)
            return false
        }

        val hasReplayGainTag = (mp3.customTag ?: byteArrayOf())
            .toString(Charsets.UTF_8)
            .contains(REPLAYGAIN_TRACK_GAIN, ignoreCase = true)
        return hasReplayGainTag
    }

    private fun Path.toRenamedFile(): RenamedFile {
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

    private fun Path.generateNewRandomName(): String = absolute().run {
        generateSequence { parent!! / "${UUID.randomUUID()}.${extension}" }
            .first { it.notExists() }
            .name
    }

    companion object {
        const val REPLAYGAIN_TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
    }

}