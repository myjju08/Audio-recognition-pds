package com.earbrief.app.engine.trigger

import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType

interface WhisperGenerator {
    fun generate(triggerType: TriggerType, context: Map<String, String>): String
}
