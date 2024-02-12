package com.github.secretx33.ankimediavolume

import com.github.secretx33.ankimediavolume.command.runCommands
import com.github.secretx33.ankimediavolume.configuration.DatabaseConfiguration
import com.github.secretx33.ankimediavolume.model.readConfiguration
import com.github.secretx33.ankimediavolume.util.openDIScope
import org.fusesource.jansi.AnsiConsole
import toothpick.ktp.extension.getInstance
import kotlin.system.exitProcess

fun main() {
    try {
        AnsiConsole.systemInstall()
        val configuration = readConfiguration() ?: return
        val scope = openDIScope(configuration)
        try {
            runCommands(scope)
        } finally {
            scope.getInstance<DatabaseConfiguration>().close()
            AnsiConsole.systemUninstall()
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        exitProcess(1)
    }
}
