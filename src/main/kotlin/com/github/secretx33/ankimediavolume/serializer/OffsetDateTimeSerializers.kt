package com.github.secretx33.ankimediavolume.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class OffsetDateTimeSerializer : JsonSerializer<OffsetDateTime>() {
    override fun serialize(value: OffsetDateTime, generator: JsonGenerator, provider: SerializerProvider) =
        generator.writeString(value.format(DateTimeFormatter.ISO_DATE_TIME))
}

class OffsetDateTimeDeserializer : JsonDeserializer<OffsetDateTime>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): OffsetDateTime =
        OffsetDateTime.parse(parser.valueAsString, DateTimeFormatter.ISO_DATE_TIME)
}