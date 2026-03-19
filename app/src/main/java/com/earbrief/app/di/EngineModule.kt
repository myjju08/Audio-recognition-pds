package com.earbrief.app.di

import com.earbrief.app.engine.vad.SileroVadEngine
import com.earbrief.app.engine.vad.VadEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindVadEngine(impl: SileroVadEngine): VadEngine
}
