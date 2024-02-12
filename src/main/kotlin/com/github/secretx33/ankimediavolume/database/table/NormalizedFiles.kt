package com.github.secretx33.ankimediavolume.database.table

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.util.UUID

@Suppress("MemberVisibilityCanBePrivate")
object NormalizedFiles : Table() {
    val ankiMediaFolderPath: Column<String> = varchar("ankiMediaFolderPath", 2048)
    val name: Column<String> = varchar("name", 2048)
    val id: Column<UUID> = uuid("id")
    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, ankiMediaFolderPath, name)
    }
}