package com.earbrief.app.engine.vad

data class VadConfig(
    val speechThreshold: Float = 0.5f,
    val silenceThreshold: Float = 0.3f,
    val speechMinFrames: Int = 3,
    val silenceMinFrames: Int = 10,
    val adaptiveSilenceMs: Long = 5000,
    val adaptiveFrameMs: Int = 64
) {
    companion object {
        const val SAMPLE_RATE = 16000
        const val FRAME_SIZE = 512
        const val FRAME_DURATION_MS = 32
    }
}
