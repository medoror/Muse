package io.github.kkoshin.muse.repo

import android.content.Context
import com.github.foodiestudio.sugar.ExperimentalSugarApi
import com.github.foodiestudio.sugar.storage.AppFileHelper
import java.io.File

/**
 * Android implementation of FileManager using AppFileHelper
 */
@OptIn(ExperimentalSugarApi::class)
class AndroidFileManager(context: Context) : FileManager {
    private val appFileHelper = AppFileHelper(context)

    private val voicesDir: File by lazy {
        appFileHelper.requireCacheDir(false).resolve("voices")
    }

    override fun getCacheDir(): File = appFileHelper.requireCacheDir(false)

    override fun getVoiceDir(voiceId: String): File =
        voicesDir
            .resolve(voiceId)
            .also {
                it.mkdirs()
            }

    override fun getPcmCacheFile(voiceId: String, phrase: String): File =
        getVoiceDir(voiceId).resolve("$phrase.pcm")
}