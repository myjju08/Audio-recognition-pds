package com.earbrief.app.engine.vad

import android.content.Context
import com.earbrief.app.domain.model.VadState

interface VadEngine {

    suspend fun initialize(context: Context)

    fun processFrame(audioFrame: ShortArray): Float

    fun getCurrentState(): VadState

    fun getSilenceDurationMs(): Long

    fun getSpeechDurationMs(): Long

    fun release()
}
