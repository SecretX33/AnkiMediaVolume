package com.github.secretx33.ankimediavolume.util

import java.util.Scanner

fun Scanner.readInt(): Int = generateSequence { nextLine() }
    .mapNotNull { it.toIntOrNull() }
    .first()

fun Scanner.readInt(validRange: IntRange): Int = generateSequence { nextLine() }
    .mapNotNull { it.toIntOrNull() }
    .filter { it in validRange }
    .first()
