package io.github.kkoshin.muse.repo

import io.github.kkoshin.muse.dashboard.Script
import java.util.UUID

/**
 * Interface for script database operations
 */
interface ScriptDao {
    suspend fun queryAllScripts(): List<Script>
    suspend fun queryScript(id: UUID): Script?
    suspend fun insertScript(script: Script)
    suspend fun deleteScript(id: UUID)
}