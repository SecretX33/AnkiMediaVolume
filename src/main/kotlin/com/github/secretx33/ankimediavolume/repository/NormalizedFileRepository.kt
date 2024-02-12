package com.github.secretx33.ankimediavolume.repository

import com.github.secretx33.ankimediavolume.database.Database
import com.github.secretx33.ankimediavolume.database.table.NormalizedFiles
import com.github.secretx33.ankimediavolume.database.table.NormalizedFiles.ankiMediaFolderPath
import com.github.secretx33.ankimediavolume.database.table.NormalizedFiles.id
import com.github.secretx33.ankimediavolume.database.table.NormalizedFiles.name
import com.github.secretx33.ankimediavolume.model.NormalizedFile
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import toothpick.InjectConstructor
import java.nio.file.Path
import javax.inject.Singleton
import kotlin.io.path.invariantSeparatorsPathString

@Singleton
@InjectConstructor
class NormalizedFileRepository(private val database: Database) {

    fun getNormalizedFilesIn(
        ankiMediaFolderPath: Path,
        fileNames: Set<String>,
    ): Set<String> {
        awaitDatabase()
        val notNormalizedFileNames = transaction {
            NormalizedFiles.selectAll()
                .where {
                    (NormalizedFiles.ankiMediaFolderPath eq ankiMediaFolderPath.invariantSeparatorsPathString)
                        .and(name inList fileNames)
                }
                .mapTo(mutableSetOf()) { it[name] }
        }
        return notNormalizedFileNames
    }

    fun insertAll(normalizedFiles: Collection<NormalizedFile>) {
        awaitDatabase()
        transaction {
            NormalizedFiles.batchInsert(normalizedFiles, shouldReturnGeneratedValues = false) { set(it) }
        }
    }

    private fun UpdateBuilder<*>.set(normalizedFile: NormalizedFile) {
        set(name, normalizedFile.name)
        set(ankiMediaFolderPath, normalizedFile.ankiMediaFolderPath.invariantSeparatorsPathString)
        set(id, normalizedFile.id)
    }

    private fun awaitDatabase() = runBlocking { database.awaitInitialization() }

}

