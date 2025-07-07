package io.github.kkoshin.muse.repo

import io.github.kkoshin.muse.dashboard.Script
import java.util.UUID

/**
 * In-memory fake implementation of ScriptDao for testing
 */
class FakeScriptDao : ScriptDao {
    private val scripts = mutableMapOf<UUID, Script>()

    override suspend fun queryAllScripts(): List<Script> = scripts.values.toList()

    override suspend fun queryScript(id: UUID): Script? = scripts[id]

    override suspend fun insertScript(script: Script) {
        scripts[script.id] = script
    }

    override suspend fun deleteScript(id: UUID) {
        scripts.remove(id)
    }
    
    // Test helper to clear all data
    fun clear() {
        scripts.clear()
    }
}