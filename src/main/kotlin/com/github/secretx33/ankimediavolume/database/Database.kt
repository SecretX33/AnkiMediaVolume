package com.github.secretx33.ankimediavolume.database

import com.github.secretx33.ankimediavolume.configuration.DatabaseConfiguration
import com.github.secretx33.ankimediavolume.database.table.NormalizedFiles
import com.github.secretx33.ankimediavolume.util.IS_DEVELOPMENT
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import toothpick.InjectConstructor
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.exposed.sql.Database as ExposedDatabase

@Singleton
@InjectConstructor
class Database(private val databaseConfiguration: DatabaseConfiguration) {

    private val state = AtomicReference(DatabaseState.NOT_INITIALIZED)
    private val databaseInitiated = CompletableDeferred<Unit>()
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    private fun initialize() {
        if (!state.compareAndSet(DatabaseState.NOT_INITIALIZED, DatabaseState.INITIALIZING)) return

        try {
            ExposedDatabase.connect(databaseConfiguration.hikariDataSource.lazy.value)
            createTables()
        } catch (e: Exception) {
            state.set(DatabaseState.ERROR)
            return handleDatabaseException(e, "An exception occurred while trying to initialize the database")
        }

        state.set(DatabaseState.INITIALIZED)
        databaseInitiated.complete(Unit)
    }

    private fun createTables() {
        transaction {
            if (IS_DEVELOPMENT) addLogger(Slf4jSqlDebugLogger, StdOutSqlLogger)
            SchemaUtils.create(NormalizedFiles)
        }
    }

    private fun handleDatabaseException(e: Throwable, logMessage: String) {
        log.error(logMessage, e)
        throw e
    }

    suspend fun awaitInitialization() = coroutineScope {
        initialize()

        var totalAwaitedTime = Duration.ZERO

        while (isActive && state.get() != DatabaseState.INITIALIZED) {
            if (state.get() == DatabaseState.ERROR) {
                throw IllegalStateException("Cannot use database because its initialization has failed.")
            }
            if (totalAwaitedTime > MAX_AWAIT_TIME) {
                throw TimeoutException("Database initialization await timeout of $MAX_AWAIT_TIME was reached")
            }

            val timedOut = withTimeoutOrNull(AWAIT_TIME_STEP) { databaseInitiated.await() } == null
            if (!timedOut) break

            totalAwaitedTime += AWAIT_TIME_STEP
        }
    }

    private enum class DatabaseState {
        NOT_INITIALIZED,
        INITIALIZING,
        INITIALIZED,
        ERROR,
    }

    private companion object {
        /**
         * Interval between two checks to see if the database was initialized.
         */
        val AWAIT_TIME_STEP = 50.milliseconds

        /**
         * The maximum amount of time that we'll await for the database to be fully initialized.
         */
        val MAX_AWAIT_TIME = 4.seconds
    }
}
