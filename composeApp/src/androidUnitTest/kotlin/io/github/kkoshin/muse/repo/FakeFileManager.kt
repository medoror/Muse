package io.github.kkoshin.muse.repo

import java.io.File

/**
 * Fake implementation of FileManager for testing
 */
class FakeFileManager(private val tempDir: File) : FileManager {
    
    override fun getCacheDir(): File = tempDir

    override fun getVoiceDir(voiceId: String): File {
        val voiceDir = tempDir.resolve("voices").resolve(voiceId)
        voiceDir.mkdirs()
        return voiceDir
    }

    override fun getPcmCacheFile(voiceId: String, phrase: String): File =
        getVoiceDir(voiceId).resolve("$phrase.pcm")
}