package com.github.secretx33.ankimediavolume.command

interface Command {
    val subCommands: Set<Command> get() = emptySet()

    fun execute() {
    }
}