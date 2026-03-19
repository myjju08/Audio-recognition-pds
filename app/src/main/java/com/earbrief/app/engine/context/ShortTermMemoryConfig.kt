package com.earbrief.app.engine.context

data class ShortTermMemoryConfig(
    val windowDurationMs: Long = 600_000L,
    val maxInterventionsPerWindow: Int = 3,
    val maxUtterances: Int = 500
)
