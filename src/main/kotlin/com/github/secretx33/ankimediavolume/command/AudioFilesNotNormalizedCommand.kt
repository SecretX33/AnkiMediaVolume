package com.github.secretx33.ankimediavolume.command

import com.mpatric.mp3agic.Mp3File
import toothpick.InjectConstructor
import java.nio.file.Path
import javax.inject.Singleton
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

@Singleton
@InjectConstructor
class AudioFilesNotNormalizedCommand : AbstractRenameMediaFilesCommand() {

    override val name: String = "Rename audio files (not normalized only)"

    override fun CommandContext.getMediaFiles(): Collection<Path> = configuration.ankiMediaFolderPath
        .listDirectoryEntries("*.mp3")
        .filter {
            NOT_ASCII_REGEX in it.name && !it.isVolumeNormalized()
        }

    private fun Path.isVolumeNormalized(): Boolean {
        val mp3 = try {
            Mp3File(toFile())
        } catch (e: Exception) {
            log.debug("File '$this' could not be parsed, assume it is not normalized", e)
            return false
        }

        val hasReplayGainTag = (mp3.customTag ?: byteArrayOf())
            .toString(Charsets.UTF_8)
            .contains(REPLAYGAIN_TRACK_GAIN, ignoreCase = true)
        return hasReplayGainTag
    }

    private companion object {
        const val REPLAYGAIN_TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
    }

}