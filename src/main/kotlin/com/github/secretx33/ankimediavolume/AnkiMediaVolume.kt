@file:Suppress("RemoveExplicitTypeArguments", "UnstableApiUsage", "RedundantSuspendModifier", "UNCHECKED_CAST")
@file:OptIn(ExperimentalTime::class, ExperimentalPathApi::class)

package com.github.secretx33.ankimediavolume

import com.github.secretx33.ankimediavolume.command.runCommands
import com.github.secretx33.ankimediavolume.model.readConfiguration
import com.github.secretx33.ankimediavolume.util.openDIScope
import kotlin.io.path.ExperimentalPathApi
import kotlin.time.ExperimentalTime

suspend fun main() {
    val configuration = readConfiguration() ?: return
    val scope = openDIScope(configuration)
    runCommands(scope)
}
