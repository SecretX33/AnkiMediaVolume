package com.github.secretx33.ankimediavolume.command

import toothpick.InjectConstructor
import toothpick.Scope

@InjectConstructor
class RenameMediaFilesCommand(private val scope: Scope): Command {
    override fun execute() {
        // Detects if media folder is already renamed, then promptly refuses to continue

        // Asks user for confirmation

        // Find which media files need to be renamed, then generate a new random UUID name for them

        // Save the new names in a JSON file within the programs folder, so we can rollback later

        // Renames media folder files
    }

    private companion object {
        val RENAME_SESSION_NAME_TEMPLATE = "rename_session_{date}.json"
        val RENAME_SESSION_DATE_FORMAT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    }
}