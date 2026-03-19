package com.earbrief.app.di

import com.earbrief.app.engine.trigger.TemplateWhisperGenerator
import com.earbrief.app.engine.trigger.TriggerRule
import com.earbrief.app.engine.trigger.WhisperGenerator
import com.earbrief.app.engine.trigger.rules.KeywordInstantTriggerRule
import com.earbrief.app.engine.trigger.rules.ScheduleGapTriggerRule
import com.earbrief.app.engine.trigger.rules.SilenceDetectTriggerRule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TriggerModule {

    @Binds
    @Singleton
    abstract fun bindWhisperGenerator(impl: TemplateWhisperGenerator): WhisperGenerator

    companion object {
        @Provides
        @Singleton
        fun provideTriggerRules(
            keywordInstantTriggerRule: KeywordInstantTriggerRule,
            silenceDetectTriggerRule: SilenceDetectTriggerRule,
            scheduleGapTriggerRule: ScheduleGapTriggerRule
        ): Set<@JvmSuppressWildcards TriggerRule> = setOf(
            keywordInstantTriggerRule,
            silenceDetectTriggerRule,
            scheduleGapTriggerRule
        )
    }
}
