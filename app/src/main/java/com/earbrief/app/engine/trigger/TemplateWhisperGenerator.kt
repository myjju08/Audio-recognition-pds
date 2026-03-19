package com.earbrief.app.engine.trigger

import com.earbrief.app.domain.model.TriggerType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateWhisperGenerator @Inject constructor() : WhisperGenerator {

    override fun generate(triggerType: TriggerType, context: Map<String, String>): String {
        return when (triggerType) {
            TriggerType.SCHEDULE_GAP -> {
                val gapMinutes = context[KEY_GAP_MINUTES] ?: "알 수 없음"
                "일정 공백이 감지되었습니다. 약 ${gapMinutes}분 여유가 있습니다."
            }

            TriggerType.SILENCE_DETECT -> {
                val silenceMs = context[KEY_SILENCE_DURATION_MS] ?: "0"
                "침묵이 감지되었습니다. 현재 침묵 시간은 ${silenceMs}ms입니다."
            }

            TriggerType.KEYWORD_INSTANT -> {
                val keyword = context[KEY_KEYWORD] ?: "키워드"
                "핵심 키워드 '${keyword}'가 감지되었습니다."
            }

            else -> {
                val detail = if (context.isEmpty()) {
                    ""
                } else {
                    context.entries
                        .sortedBy { it.key }
                        .joinToString(separator = ", ", prefix = " (", postfix = ")") { "${it.key}=${it.value}" }
                }
                "${triggerType.name} 트리거가 감지되었습니다.${detail}"
            }
        }
    }

    private companion object {
        const val KEY_GAP_MINUTES = "gapMinutes"
        const val KEY_SILENCE_DURATION_MS = "silenceDurationMs"
        const val KEY_KEYWORD = "keyword"
    }
}
