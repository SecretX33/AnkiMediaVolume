package com.github.secretx33.ankimediavolume.util

import org.slf4j.LoggerFactory
import toothpick.configuration.Configuration
import java.lang.invoke.MethodHandles

private val IS_DEVELOPMENT = System.getenv("IS_DEVELOP").toBoolean()

fun getEnvConfiguration(): Configuration =
    when (IS_DEVELOPMENT) {
        true -> {
            val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
            log.info("[AnkiMediaVolume] Running in development mode.")
            Configuration.forDevelopment()
        }
        false -> Configuration.forProduction()
    }