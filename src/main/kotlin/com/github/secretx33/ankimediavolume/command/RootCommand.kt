package com.github.secretx33.ankimediavolume.command

import toothpick.InjectConstructor
import toothpick.Scope
import javax.inject.Singleton

@Singleton
@InjectConstructor
class RootCommand(scope: Scope) : CommandGroup {

    override val name: String = "Anki Media Volume"

    override val subCommands: Set<Command> = setOf(
        RenameMediaFilesCommand::class,
        ListSessionsForUndoCommand::class,
    ).mapTo(mutableSetOf()) { scope.getInstance(it.java) }

}