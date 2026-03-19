package com.earbrief.app.di

import com.earbrief.app.engine.context.InMemoryShortTermMemory
import com.earbrief.app.engine.context.ContextEngine
import com.earbrief.app.engine.context.DefaultContextEngine
import com.earbrief.app.engine.context.EntityExtractor
import com.earbrief.app.engine.context.KoreanDateTimeParser
import com.earbrief.app.engine.context.KoreanEntityExtractor
import com.earbrief.app.engine.context.KoreanQuestionDetector
import com.earbrief.app.engine.context.QuestionDetector
import com.earbrief.app.engine.context.ShortTermMemory
import com.earbrief.app.engine.context.ShortTermMemoryConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ContextModule {

    @Binds
    @Singleton
    abstract fun bindShortTermMemory(impl: InMemoryShortTermMemory): ShortTermMemory

    @Binds
    @Singleton
    abstract fun bindEntityExtractor(impl: KoreanEntityExtractor): EntityExtractor

    @Binds
    @Singleton
    abstract fun bindQuestionDetector(impl: KoreanQuestionDetector): QuestionDetector

    @Binds
    @Singleton
    abstract fun bindContextEngine(impl: DefaultContextEngine): ContextEngine

    companion object {
        @Provides
        @Singleton
        fun provideShortTermMemoryConfig(): ShortTermMemoryConfig = ShortTermMemoryConfig()

        @Provides
        @Singleton
        fun provideKoreanDateTimeParser(): KoreanDateTimeParser = KoreanDateTimeParser()
    }
}
