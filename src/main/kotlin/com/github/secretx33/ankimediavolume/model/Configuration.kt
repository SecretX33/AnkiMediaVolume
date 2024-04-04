package com.github.secretx33.ankimediavolume.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.secretx33.ankimediavolume.util.CURRENT_OS
import com.github.secretx33.ankimediavolume.util.OS
import com.github.secretx33.ankimediavolume.util.createFileIfNotExists
import com.github.secretx33.ankimediavolume.util.objectMapper
import com.github.secretx33.ankimediavolume.util.prettyObjectMapper
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.readAttributes
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val log by lazy { LoggerFactory.getLogger(MethodHandles.lookup().lookupClass()) }

private val CONFIG_PATH = Path("config.json").absolute()

data class Configuration(
    val ankiMediaFolderPath: Path = getAnkiMediaPath(),
    val temporaryAudiosFolderPath: Path = Path("audios_to_rename"),
) {
    @JsonIgnore
    val ankiMediaLockFile = ankiMediaFolderPath / LOCK_FILE_NAME
}

private const val LOCK_FILE_NAME = "anki-media-volume.lock"

private const val ANKI_MEDIA_FOLDER_NAME = "collection.media"

private fun getAnkiMediaPath(): Path {
    val basePath = getAnkiBasePath()
    val latestModifiedUser = basePath.listDirectoryEntries()
        .filter { it.isDirectory() && it.name != "addon21" }
        .maxBy { it.readAttributes<BasicFileAttributes>().lastModifiedTime() }
    return latestModifiedUser / ANKI_MEDIA_FOLDER_NAME
}

private fun getAnkiBasePath(): Path {
    val userHome = Path(System.getProperty("user.home"))
    return when (CURRENT_OS) {
        OS.WINDOWS -> userHome / "AppData\\Roaming\\Anki2"
        OS.MAC -> userHome / "Library/Application Support/Anki2"
        OS.LINUX -> userHome / ".local/share/Anki2"
    }
}

fun readConfiguration(): Configuration? {
    if (CONFIG_PATH.notExists()) {
        log.info("Configuration file not found, creating default one.\n\nPlease check the generated configuration values, then re-run the program.")
        CONFIG_PATH.createFileIfNotExists().writeText(prettyObjectMapper.writeValueAsString(Configuration()))
        readlnOrNull()
        return null
    }

    return try {
        objectMapper.readValue<Configuration>(CONFIG_PATH.readText())
    } catch (e: Exception) {
        log.error("Error parsing the configuration file '${CONFIG_PATH.absolutePathString()}', please fix it and re-run the program.", e)
        readlnOrNull()
        null
    }
}