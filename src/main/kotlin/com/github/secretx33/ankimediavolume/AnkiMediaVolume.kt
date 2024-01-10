package com.github.secretx33.ankimediavolume

import com.github.secretx33.ankimediavolume.command.runCommands
import com.github.secretx33.ankimediavolume.model.readConfiguration
import com.github.secretx33.ankimediavolume.util.openDIScope
import org.fusesource.jansi.AnsiConsole
import kotlin.system.exitProcess

fun main() {
    try {
        AnsiConsole.systemInstall()
        val configuration = readConfiguration() ?: return
        val scope = openDIScope(configuration)
        runCommands(scope)
    } catch (e: Throwable) {
        e.printStackTrace()
        exitProcess(1)
    }
}
