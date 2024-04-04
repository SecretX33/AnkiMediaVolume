package com.github.secretx33.ankimediavolume.command

import com.github.secretx33.ankimediavolume.util.findRegistrableClasses
import toothpick.InjectConstructor
import toothpick.Scope
import javax.inject.Singleton

@Singleton
@InjectConstructor
class RootCommand(scope: Scope) : CommandGroup {

    override val name: String = "Anki Media Volume"

    override val subCommands: Set<Command> = findRegistrableClasses<Command>(this::class.java.packageName)
        .filter { it != this::class }
        .mapTo(mutableSetOf()) { scope.getInstance(it.java) }

}