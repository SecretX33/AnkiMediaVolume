package com.github.secretx33.ankimediavolume.model

import java.nio.file.Path
import java.util.UUID

data class NormalizedFile(
    val ankiMediaFolderPath: Path,
    val name: String,
    val id: UUID = UUID.randomUUID(),
)
