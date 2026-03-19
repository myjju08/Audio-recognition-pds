package com.earbrief.app.di

import com.earbrief.app.engine.whisper.WhisperTimingConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WhisperModule {

    @Provides
    @Singleton
    fun provideWhisperTimingConfig(): WhisperTimingConfig = WhisperTimingConfig()
}
