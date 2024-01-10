package com.github.secretx33.ankimediavolume.model

import com.github.secretx33.ankimediavolume.util.setTimes
import java.nio.file.Path
import java.time.Instant
import java.time.OffsetDateTime

data class RenameSession(
    val date: OffsetDateTime,
    val ankiMediaFolderPath: Path,
    val files: Set<RenamedFile>,
) {
    init {
        require(files.isNotEmpty()) { "Files cannot be empty" }
    }
}

data class RenamedFile(
    val originalName: String,
    val renamedName: String,
    val fileAttributesInfo: FileAttributesInfo,
)

data class FileAttributesInfo(
    val createdAt: Instant,
    val lastModifiedAt: Instant,
)

fun Path.setTimes(fileAttribute: FileAttributesInfo) = setTimes(
    createdAt = fileAttribute.createdAt,
    lastModifiedAt = fileAttribute.lastModifiedAt,
)