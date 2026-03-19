package com.earbrief.app.engine.context

import com.earbrief.app.domain.model.Entity

interface EntityExtractor {
    fun extract(text: String, referenceTimeMs: Long = System.currentTimeMillis()): List<Entity>
}
