package com.github.secretx33.ankimediavolume.command

import com.github.secretx33.ankimediavolume.model.Configuration
import com.github.secretx33.ankimediavolume.util.cleanScreen
import com.github.secretx33.ankimediavolume.util.readInt
import com.github.secretx33.ankimediavolume.util.shiftBy
import com.github.secretx33.ankimediavolume.util.shiftEndBy
import org.slf4j.LoggerFactory
import toothpick.Scope
import toothpick.ktp.extension.getInstance
import java.lang.invoke.MethodHandles
import java.util.Scanner

private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

suspend fun runCommands(scope: Scope) {
    val context = CommandContext(configuration = scope.getInstance<Configuration>())
    val stack = ArrayDeque<Command>().apply { addFirst(scope.getInstance<RootCommand>()) }

    while (stack.isNotEmpty()) {
        val command = stack.removeFirst()

        when (val action = context.runCommand(command, context, stack.size)) {
            is Action.DownOneLevel -> {
                stack.addFirst(action.commandToReturnTo)
                stack.addFirst(action.commandToVisit)
            }
            is Action.Return -> {}
        }
    }
}

private suspend fun CommandContext.runCommand(
    command: Command,
    context: CommandContext,
    currentLevel: Int,
): Action {
    when (command) {
        is ExecutionCommand -> {
            cleanScreen()
            command.apply { execute() }
            return Action.Return
        }
        is CommandGroup -> {
            val selectedOption = presentOptions(
                command = command,
                subCommands = command.subCommands.toList(),
                scanner = context.scanner,
                currentLevel = currentLevel
            ) ?: return Action.Return

            return Action.DownOneLevel(commandToVisit = selectedOption, commandToReturnTo = command)
        }
    }
}

private fun presentOptions(
    command: CommandGroup,
    subCommands: List<Command>,
    scanner: Scanner,
    currentLevel: Int,
): Command? {
    val returnOptionIndex = subCommands.size + 1

    val optionText = subCommands.mapIndexed { index, item ->
        "${index + 1}. ${item.name}"
    }.plus("$returnOptionIndex. ${if (currentLevel == 0) "Exit" else "Return"}")
        .joinToString("\n", prefix = "${command.name}:\n\n")

    cleanScreen()
    log.info(optionText)

    val options = scanner.readInt(subCommands.indices.shiftBy(1).shiftEndBy(1))
    if (options == returnOptionIndex) return null

    return subCommands[options - 1]
}

private interface Action {
    data class DownOneLevel(val commandToVisit: Command, val commandToReturnTo: Command) : Action
    data object Return : Action
}