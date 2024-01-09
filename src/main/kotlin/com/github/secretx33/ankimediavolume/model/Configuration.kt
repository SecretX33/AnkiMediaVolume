package com.github.secretx33.ankimediavolume.model

import com.github.secretx33.ankimediavolume.util.CURRENT_OS
import com.github.secretx33.ankimediavolume.util.OS
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readAttributes

data class Configuration(
    val ankiMediaFolderPath: Path = getAnkiMediaPath(),
    val rollbackFolderPath: Path = Path(""),
)

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
