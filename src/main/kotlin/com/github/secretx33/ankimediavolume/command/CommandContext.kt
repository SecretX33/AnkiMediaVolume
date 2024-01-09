package com.github.secretx33.ankimediavolume.command

import com.github.secretx33.ankimediavolume.model.Configuration
import java.util.Scanner

data class CommandContext(
    val scanner: Scanner = Scanner(System.`in`, Charsets.UTF_8),
    val configuration: Configuration,
)
