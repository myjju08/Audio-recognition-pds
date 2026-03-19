package com.earbrief.app.domain.model

data class TimeRange(
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = endMs - startMs
}
