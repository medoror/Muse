package io.github.kkoshin.muse.repo

import java.io.File

/**
 * Interface for file management operations
 */
interface FileManager {
    fun getCacheDir(): File
    fun getVoiceDir(voiceId: String): File
    fun getPcmCacheFile(voiceId: String, phrase: String): File
}