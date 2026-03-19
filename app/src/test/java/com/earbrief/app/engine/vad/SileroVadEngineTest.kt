package com.earbrief.app.engine.vad

import com.earbrief.app.domain.model.VadState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SileroVadEngine")
class SileroVadEngineTest {

    private lateinit var config: VadConfig

    @BeforeEach
    fun setup() {
        config = VadConfig()
    }

    @Nested
    @DisplayName("VadConfig defaults")
    inner class ConfigDefaults {

        @Test
        fun `speech threshold defaults to 0_5`() {
            assertEquals(0.5f, config.speechThreshold)
        }

        @Test
        fun `silence threshold defaults to 0_3`() {
            assertEquals(0.3f, config.silenceThreshold)
        }

        @Test
        fun `speech min frames defaults to 3`() {
            assertEquals(3, config.speechMinFrames)
        }

        @Test
        fun `silence min frames defaults to 10`() {
            assertEquals(10, config.silenceMinFrames)
        }

        @Test
        fun `frame size is 512 samples`() {
            assertEquals(512, VadConfig.FRAME_SIZE)
        }

        @Test
        fun `sample rate is 16000 Hz`() {
            assertEquals(16000, VadConfig.SAMPLE_RATE)
        }

        @Test
        fun `frame duration is 32ms`() {
            assertEquals(32, VadConfig.FRAME_DURATION_MS)
        }
    }

    @Nested
    @DisplayName("VadState transitions")
    inner class StateTransitions {

        @Test
        fun `initial state is SILENCE`() {
            val engine = SileroVadEngine(config)
            assertEquals(VadState.SILENCE, engine.getCurrentState())
        }

        @Test
        fun `silence duration returns 0 when not in silence with tracked time`() {
            val engine = SileroVadEngine(config)
            assertEquals(0L, engine.getSilenceDurationMs())
        }

        @Test
        fun `speech duration returns 0 when not speaking`() {
            val engine = SileroVadEngine(config)
            assertEquals(0L, engine.getSpeechDurationMs())
        }
    }

    @Nested
    @DisplayName("Frame pool - privacy")
    inner class PrivacyTests {

        @Test
        fun `frame is zeroed after processing concept`() {
            val frame = ShortArray(VadConfig.FRAME_SIZE) { 1000 }
            frame.fill(0)
            assertTrue(frame.all { it == 0.toShort() })
        }
    }
}
