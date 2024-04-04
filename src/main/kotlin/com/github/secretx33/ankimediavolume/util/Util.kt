package com.github.secretx33.ankimediavolume.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import com.github.secretx33.ankimediavolume.model.Configuration
import com.github.secretx33.ankimediavolume.serializer.InstantDeserializer
import com.github.secretx33.ankimediavolume.serializer.InstantSerializer
import com.github.secretx33.ankimediavolume.serializer.OffsetDateTimeDeserializer
import com.github.secretx33.ankimediavolume.serializer.OffsetDateTimeSerializer
import com.github.secretx33.ankimediavolume.serializer.PathDeserializer
import com.github.secretx33.ankimediavolume.serializer.PathSerializer
import com.google.common.reflect.ClassPath
import org.fusesource.jansi.Ansi
import java.awt.Desktop
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileAttributesView
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun Any.getResourceAsByteArray(path: String): ByteArray = this::class.java.classLoader
    .getResourceAsStream(path)
    ?.buffered()
    ?.use { it.readBytes() }
    ?: throw IllegalArgumentException("Resource not found: $path")

val objectMapper: ObjectMapper by lazy {
    ObjectMapper().findAndRegisterModules()
        .registerModules(KotlinModule.Builder().build())
        .applyProjectDefaults()
}

val prettyObjectMapper: ObjectWriter by lazy { objectMapper.writerWithDefaultPrettyPrinter() }

private fun ObjectMapper.applyProjectDefaults(): ObjectMapper = apply {
    registerModule(SimpleModule().apply {
        addSerializer(Path::class, PathSerializer())
        addDeserializer(Path::class, PathDeserializer())
        addSerializer(OffsetDateTime::class, OffsetDateTimeSerializer())
        addDeserializer(OffsetDateTime::class, OffsetDateTimeDeserializer())
        addSerializer(Instant::class, InstantSerializer())
        addDeserializer(Instant::class, InstantDeserializer())
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

fun Path.moveToTrash() {
    if (!Desktop.isDesktopSupported()) {
        deleteIfExists()  // If platform doesn't support Desktop, just delete the file instead
        return
    }
    val desktop = Desktop.getDesktop()
    try {
        desktop.moveToTrash(toFile())
    } catch (e: Exception) {
        when (e) {
            is UnsupportedOperationException -> deleteIfExists()  // If platform doesn't support moving files to trash, just delete the file instead
            is IllegalArgumentException -> {} // If file doesn't exist, that's OK
            else -> throw e
        }
    }
}

infix fun IntRange.shiftBy(number: Int): IntRange = IntRange(start + number, last + number)

infix fun IntRange.shiftEndBy(number: Int): IntRange = IntRange(start, last + number)

fun Path.setTimes(
    createdAt: Instant? = null,
    lastModifiedAt: Instant? = null,
    lastAccessedAt: Instant? = null,
) = fileAttributesView<BasicFileAttributeView>().setTimes(
    lastModifiedAt?.let(FileTime::from),
    lastAccessedAt?.let(FileTime::from),
    createdAt?.let(FileTime::from),
)

fun cleanScreen() = print(Ansi.ansi().cursorUp(Integer.MAX_VALUE).eraseScreen())

val KClass<*>.isConcreteType: Boolean
    get() = !Modifier.isAbstract(java.modifiers) && !Modifier.isInterface(java.modifiers)

@Suppress("UNCHECKED_CAST")
inline fun <T : Any> findClasses(pkg: String, filter: (KClass<*>) -> Boolean): Set<KClass<T>> =
    ClassPath.from(Configuration::class.java.classLoader)
        .getTopLevelClassesRecursive(pkg)
        .map { it.load().kotlin }
        .filterTo(mutableSetOf(), filter) as Set<KClass<T>>

inline fun <reified T : Any> findRegistrableClasses(pkg: String): Set<KClass<T>> =
    findClasses(pkg) { it.isConcreteType && it.isSubclassOf(T::class) }