package com.github.secretx33.ankimediavolume.util

/**
 * Enum with list of all three major Operational Systems.
 *
 * Can be used to create conditional code to adapt to different systems.
 */
enum class OS {
    WINDOWS,
    LINUX,
    MAC,
}

/**
 * Can be used to determine in what OS is the instance running on.
 */
val CURRENT_OS: OS = run {
    val osName = System.getProperty("os.name").orEmpty()
    when {
        osName.contains("windows", ignoreCase = true) -> OS.WINDOWS
        osName.contains("mac", ignoreCase = true)
            || osName.contains("darwin", ignoreCase = true) -> OS.MAC
        else -> OS.LINUX
    }
}

val IS_OS_WINDOWS = CURRENT_OS == OS.WINDOWS

val IS_OS_MAC_OS = CURRENT_OS == OS.MAC

val IS_OS_LINUX = CURRENT_OS == OS.LINUX