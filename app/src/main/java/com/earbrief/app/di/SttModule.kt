package com.earbrief.app.di

import com.earbrief.app.data.remote.deepgram.DeepgramSttEngine
import com.earbrief.app.engine.stt.SttEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SttModule {

    @Binds
    @Singleton
    abstract fun bindSttEngine(impl: DeepgramSttEngine): SttEngine
}
