package com.github.secretx33.ankimediavolume.configuration

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.secretx33.ankimediavolume.util.getResourceAsByteArray
import com.github.secretx33.ankimediavolume.util.objectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import toothpick.InjectConstructor
import java.io.Closeable
import javax.inject.Singleton
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.time.Duration.Companion.milliseconds

@Singleton
@InjectConstructor
class DatabaseConfiguration : Closeable {

    private val dataSourceConfig by lazy {
        objectMapper.readValue<DataSourceConfiguration>(getResourceAsByteArray(DATASOURCE_PROPERTIES_FILE))
    }
    private val hikariProperties by lazy { dataSourceConfig.hikariProperties }
    private val databaseFile by lazy { Path("database", "history").absolute().createParentDirectories() }

    private val hikariConfig by lazy {
        HikariConfig().apply {
            jdbcUrl = hikariProperties.jdbcUrl.replace("<filePath>", databaseFile.absolutePathString())
            maximumPoolSize = hikariProperties.maximumPoolSize
            transactionIsolation = hikariProperties.transactionIsolation
            driverClassName = hikariProperties.driverClassName
            dataSourceConfig.properties.forEach { (propertyName, value) ->
                addDataSourceProperty(propertyName, value)
            }
        }
    }

    val hikariDataSource: HikariDataSourceProvider = HikariDataSourceProvider(lazy { HikariDataSource(hikariConfig) })

    override fun close() {
        val dataSource = hikariDataSource.lazy.takeIf { it.isInitialized() }?.value ?: return
        runBlocking {
            withTimeoutOrNull(1500.milliseconds) {
                runInterruptible(Dispatchers.IO) { dataSource.close() }
            }
        }
    }

    private data class DataSourceConfiguration(
        val hikariProperties: HikariProperties,
        val properties: Map<String, Any>,
    )

    data class HikariProperties(
        val jdbcUrl: String,
        val maximumPoolSize: Int,
        val transactionIsolation: String,
        val driverClassName: String,
    )

    private companion object {
        const val DATASOURCE_PROPERTIES_FILE = "database/datasource_properties.json"
    }
}

class HikariDataSourceProvider(val lazy: Lazy<HikariDataSource>)
