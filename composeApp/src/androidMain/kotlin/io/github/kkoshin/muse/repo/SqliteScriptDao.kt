package io.github.kkoshin.muse.repo

import android.content.Context
import io.github.kkoshin.muse.dashboard.Script
import io.github.kkoshin.muse.database.AppDatabase
import io.github.kkoshin.muse.repo.DriverFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * SQLite implementation of ScriptDao using SQLDelight
 */
class SqliteScriptDao(context: Context) : ScriptDao {
    private val database = AppDatabase(DriverFactory(context).createDriver())
    private val scriptQueries = database.scriptQueries

    override suspend fun queryAllScripts(): List<Script> = withContext(Dispatchers.IO) {
        scriptQueries.queryAllScripts().executeAsList().map {
            Script(UUID.fromString(it.id), it.title, it.text, it.created_At)
        }
    }

    override suspend fun queryScript(id: UUID): Script? = withContext(Dispatchers.IO) {
        scriptQueries.queryScirptById(id.toString()).executeAsOneOrNull()?.let {
            Script(UUID.fromString(it.id), it.title, it.text, it.created_At)
        }
    }

    override suspend fun insertScript(script: Script) = withContext(Dispatchers.IO) {
        scriptQueries.insertScript(script.id.toString(), script.title, script.text, script.createAt)
    }

    override suspend fun deleteScript(id: UUID) = withContext(Dispatchers.IO) {
        scriptQueries.deleteScriptById(id.toString())
    }
}