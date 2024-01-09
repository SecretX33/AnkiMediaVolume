package com.github.secretx33.ankimediavolume.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.module.SimpleModule
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

val objectMapper: ObjectMapper by lazy { ObjectMapper().findAndRegisterModules().applyProjectDefaults() }

val prettyObjectMapper: ObjectWriter by lazy { objectMapper.writerWithDefaultPrettyPrinter() }

private fun ObjectMapper.applyProjectDefaults(): ObjectMapper = apply {
    registerModule(SimpleModule().apply {
        addAbstractTypeMapping(Set::class.java, LinkedHashSet::class.java)
    })
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
}

fun Path.createFileIfNotExists(): Path {
    if (exists()) return this
    parent?.createDirectories()
    return createFile()
}