package com.github.secretx33.ankimediavolume.model

import java.nio.file.Path
import java.time.Instant
import java.time.OffsetDateTime

data class RenameSession(
    val date: OffsetDateTime,
    val ankiMediaFolderPath: Path,
    val files: Set<RenamedFile>,
)

data class RenamedFile(
    val path: Path,
    val fileAttributesInfo: FileAttributesInfo,
)

data class FileAttributesInfo(
    val createdAt: Instant,
    val lastModifiedAt: Instant,
)