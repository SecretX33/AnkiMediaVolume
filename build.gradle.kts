import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.22"
    kotlin("kapt") version "1.9.22"
    application
}

group = "com.secretx33"
version = "0.1"

val javaVersion = 17

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.github.secretx33:path-matching-resource-pattern-resolver:0.1")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.15.2"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    val toothpick_version = "3.1.0"
    implementation("com.github.stephanenicolas.toothpick:ktp:$toothpick_version")
    kapt("com.github.stephanenicolas.toothpick:toothpick-compiler:$toothpick_version")
    implementation("org.fusesource.jansi:jansi:2.4.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar { enabled = false }

artifacts.archives(tasks.shadowJar)

tasks.shadowJar {
    archiveFileName.set("${rootProject.name}.jar")
}

tasks.withType<JavaCompile> {
    options.apply {
        release.set(javaVersion)
        options.encoding = "UTF-8"
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = javaVersion.toString()
    }
}

val mainClassName = "com.github.secretx33.ankimediavolume.AnkiMediaVolumeKt"

application {
    mainClass.set(mainClassName)
}
