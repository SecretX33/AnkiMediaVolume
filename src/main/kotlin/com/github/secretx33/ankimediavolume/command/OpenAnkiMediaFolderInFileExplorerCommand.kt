package com.github.secretx33.ankimediavolume.command

import org.slf4j.LoggerFactory
import toothpick.InjectConstructor
import java.awt.Desktop
import javax.inject.Singleton
import kotlin.io.path.notExists

@Singleton
@InjectConstructor
class OpenAnkiMediaFolderInFileExplorerCommand : ExecutionCommand {

    override val name: String = "Open anki media folder in file explorer"

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun CommandContext.execute() {
        if (configuration.ankiMediaFolderPath.notExists()) {
            log.error("Anki media folder does not exist on configured path '${configuration.ankiMediaFolderPath}'.\n\nPlease make sure you have Anki installed, that you have opened it at least once, and that the configured Anki media folder actually exists, then try again.")
            scanner.nextLine()
            return
        }
        Desktop.getDesktop().open(configuration.ankiMediaFolderPath.toFile())
    }

}