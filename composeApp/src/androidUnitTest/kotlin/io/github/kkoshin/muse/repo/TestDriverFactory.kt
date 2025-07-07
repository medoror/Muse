package io.github.kkoshin.muse.repo

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kkoshin.muse.database.AppDatabase

/**
 * Test driver factory that creates in-memory SQLite databases for testing
 */
class TestDriverFactory {
    fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return driver
    }
}