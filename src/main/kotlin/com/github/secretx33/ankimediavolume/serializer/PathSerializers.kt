package com.github.secretx33.ankimediavolume.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class PathSerializer : JsonSerializer<Path>() {
    override fun serialize(path: Path, generator: JsonGenerator, provider: SerializerProvider) = generator.writeString(path.absolutePathString())
}

class PathDeserializer : JsonDeserializer<Path>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Path = Path(parser.valueAsString)
}