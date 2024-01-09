package com.github.secretx33.ankimediavolume.command

import toothpick.InjectConstructor

@InjectConstructor
class RenameMediaFilesCommand : ExecutionCommand {
    override suspend fun CommandContext.execute() {
        // Detects if media folder is already renamed, then promptly refuses to continue

        // Asks user for confirmation

        // Find which media files need to be renamed, then generate a new random UUID name for them

        // Save the new names in a JSON file within the programs folder, so we can rollback later

        // Renames media folder files
    }

}