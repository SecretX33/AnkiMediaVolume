package com.github.secretx33.ankimediavolume.util

import java.util.Locale
import java.util.Scanner

fun Scanner.readInt(): Int = generateSequence { nextLine() }
    .mapNotNull { it.toIntOrNull() }
    .first()

fun Scanner.readInt(validRange: IntRange): Int = generateSequence { nextLine() }
    .mapNotNull { it.toIntOrNull() }
    .first { it in validRange }

fun Scanner.readOption(
    options: Set<String>,
    ignoreCase: Boolean = false,
    forceLowercase: Boolean = false,
): String = generateSequence { nextLine() }
    .mapNotNull { line ->
        line.takeIf { input ->
            options.any { option -> input.equals(option, ignoreCase = ignoreCase) }
        }
    }
    .map { if (forceLowercase) it.lowercase(Locale.ROOT) else it }
    .first()
