package com.github.secretx33.ankimediavolume.command

sealed interface Command {
    val name: String
}

interface CommandGroup : Command {
    val subCommands: Set<Command>
}

interface ExecutionCommand : Command {
    suspend fun CommandContext.execute()
}