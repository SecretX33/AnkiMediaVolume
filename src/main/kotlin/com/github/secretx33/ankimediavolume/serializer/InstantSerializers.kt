package com.github.secretx33.ankimediavolume.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.Instant

class InstantSerializer : JsonSerializer<Instant>() {
    override fun serialize(value: Instant, generator: JsonGenerator, provider: SerializerProvider) =
        generator.writeNumber(value.toEpochMilli())
}

class InstantDeserializer : JsonDeserializer<Instant>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Instant =
        Instant.ofEpochMilli(parser.valueAsLong)
}