package com.github.secretx33.ankimediavolume.command

import toothpick.InjectConstructor
import java.nio.file.Path
import javax.inject.Singleton
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

@Singleton
@InjectConstructor
class AudioFilesCommand : AbstractRenameMediaFilesCommand() {

    override val name: String = "Rename audio files"

    override fun CommandContext.getMediaFiles(): Collection<Path> = configuration.ankiMediaFolderPath
        .listDirectoryEntries("*.mp3")
        .filter { NOT_ASCII_REGEX in it.name }

}