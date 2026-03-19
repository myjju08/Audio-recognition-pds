package com.earbrief.app.engine.trigger

import com.earbrief.app.domain.model.TriggerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TemplateWhisperGenerator")
class TemplateWhisperGeneratorTest {

    private val generator = TemplateWhisperGenerator()

    @Test
    fun `same input returns same output`() {
        val context = mapOf("keyword" to "긴급", "speaker" to "SELF")

        val first = generator.generate(TriggerType.KEYWORD_INSTANT, context)
        val second = generator.generate(TriggerType.KEYWORD_INSTANT, context)

        assertEquals(first, second)
    }

    @Test
    fun `schedule gap uses template`() {
        val text = generator.generate(
            triggerType = TriggerType.SCHEDULE_GAP,
            context = mapOf("gapMinutes" to "15")
        )

        assertEquals("일정 공백이 감지되었습니다. 약 15분 여유가 있습니다.", text)
    }

    @Test
    fun `silence detect uses template`() {
        val text = generator.generate(
            triggerType = TriggerType.SILENCE_DETECT,
            context = mapOf("silenceDurationMs" to "3000")
        )

        assertEquals("침묵이 감지되었습니다. 현재 침묵 시간은 3000ms입니다.", text)
    }

    @Test
    fun `keyword instant uses template`() {
        val text = generator.generate(
            triggerType = TriggerType.KEYWORD_INSTANT,
            context = mapOf("keyword" to "중요")
        )

        assertEquals("핵심 키워드 '중요'가 감지되었습니다.", text)
    }

    @Test
    fun `unknown type falls back to deterministic sorted context`() {
        val text = generator.generate(
            triggerType = TriggerType.UNKNOWN_TERM,
            context = mapOf("b" to "2", "a" to "1")
        )

        assertEquals("UNKNOWN_TERM 트리거가 감지되었습니다. (a=1, b=2)", text)
    }
}
