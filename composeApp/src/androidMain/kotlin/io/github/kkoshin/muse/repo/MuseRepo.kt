package io.github.kkoshin.muse.repo

import android.content.Context
import io.github.kkoshin.muse.R
import io.github.kkoshin.muse.dashboard.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Repository for managing scripts and file operations
 * Uses dependency injection for better testability
 */
class MuseRepo(
    private val scriptDao: ScriptDao,
    private val fileManager: FileManager,
) {
    fun getPcmCache(
        voiceId: String,
        phrase: String,
    ): File = fileManager.getPcmCacheFile(voiceId, phrase)

    suspend fun queryAllScripts(): List<Script> = scriptDao.queryAllScripts()

    suspend fun queryScript(id: UUID): Script? = scriptDao.queryScript(id)

    suspend fun insertScript(script: Script) = scriptDao.insertScript(script)

    suspend fun deleteScript(id: UUID) = scriptDao.deleteScript(id)

    companion object {
        fun getExportRelativePath(appContext: Context): String =
            "Download/${appContext.getString(R.string.app_name)}"
    }
}

// TODO: 文本过大，加载会很慢，甚至导致 ANR，暂时限定文本长度
const val MAX_TEXT_LENGTH = 25_000

suspend fun MuseRepo.queryPhrases(scriptId: UUID): List<String>? =
    withContext(Dispatchers.Default) {
        queryScript(scriptId)?.text?.take(MAX_TEXT_LENGTH)
            ?.split(' ', '\n')?.filter { it.isNotBlank() }
    }