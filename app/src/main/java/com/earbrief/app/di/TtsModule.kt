package com.earbrief.app.di

import android.content.Context
import com.earbrief.app.data.remote.elevenlabs.ElevenLabsTtsEngine
import com.earbrief.app.engine.tts.TtsConfig
import com.earbrief.app.engine.tts.TtsDiskCache
import com.earbrief.app.engine.tts.TtsEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TtsModule {

    @Provides
    @Singleton
    fun provideTtsConfig(): TtsConfig = TtsConfig()

    @Provides
    @Singleton
    fun provideTtsCacheDir(@ApplicationContext context: Context): File {
        return File(context.cacheDir, "tts")
    }

    @Provides
    @Singleton
    fun provideTtsEngine(
        elevenLabsTtsEngine: ElevenLabsTtsEngine,
        ttsCacheDir: File
    ): TtsEngine {
        return TtsDiskCache(
            delegate = elevenLabsTtsEngine,
            cacheDir = ttsCacheDir
        )
    }
}
