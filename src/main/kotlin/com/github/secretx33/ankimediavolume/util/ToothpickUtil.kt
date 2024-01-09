package com.github.secretx33.ankimediavolume.util

import com.github.secretx33.ankimediavolume.model.Configuration
import org.slf4j.LoggerFactory
import toothpick.Scope
import toothpick.ktp.KTP
import toothpick.ktp.binding.bind
import toothpick.ktp.binding.module
import java.lang.invoke.MethodHandles
import toothpick.configuration.Configuration as ToothpickConfiguration

private val IS_DEVELOPMENT = System.getenv("IS_DEVELOPMENT").toBoolean()

private fun getEnvConfiguration(): ToothpickConfiguration =
    when (IS_DEVELOPMENT) {
        true -> {
            val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
            log.info("[AnkiMediaVolume] Running in development mode.")
            ToothpickConfiguration.forDevelopment()
        }
        false -> ToothpickConfiguration.forProduction()
    }

fun openDIScope(configuration: Configuration): Scope {
    KTP.setConfiguration(getEnvConfiguration())
    val scope = KTP.openRootScope().run {
        installModules(module {
            bind<Scope>().toInstance(this@run)
            bind<Configuration>().toInstance(configuration)
        })
    }
    return scope
}
