package com.earbrief.app.engine.vad

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.earbrief.app.domain.model.VadState
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SileroVadEngine @Inject constructor(
    private val config: VadConfig
) : VadEngine {

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    private var state: FloatArray = FloatArray(2 * 1 * 128)
    private var contextBuffer: FloatArray = FloatArray(CONTEXT_SIZE)

    private var currentState: VadState = VadState.SILENCE
    private var speechFrameCount: Int = 0
    private var silenceFrameCount: Int = 0
    private var silenceStartTimeMs: Long = 0L
    private var speechStartTimeMs: Long = 0L
    private var isInitialized: Boolean = false

    override suspend fun initialize(context: Context) {
        if (isInitialized) return

        ortEnv = OrtEnvironment.getEnvironment()
        val modelBytes = context.assets.open(MODEL_FILE_NAME).use { it.readBytes() }
        ortSession = ortEnv?.createSession(modelBytes)
        resetStates()
        isInitialized = true
    }

    override fun processFrame(audioFrame: ShortArray): Float {
        val session = ortSession ?: return 0f
        val env = ortEnv ?: return 0f

        require(audioFrame.size == VadConfig.FRAME_SIZE) {
            "Silero VAD v5 expects exactly ${VadConfig.FRAME_SIZE} samples for 16kHz input"
        }

        val chunk = FloatArray(audioFrame.size) { audioFrame[it] / 32768.0f }
        val inputWithContext = FloatArray(CONTEXT_SIZE + chunk.size)
        System.arraycopy(contextBuffer, 0, inputWithContext, 0, CONTEXT_SIZE)
        System.arraycopy(chunk, 0, inputWithContext, CONTEXT_SIZE, chunk.size)

        val inputTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(inputWithContext),
            longArrayOf(1, inputWithContext.size.toLong())
        )
        val srTensor = OnnxTensor.createTensor(env, longArrayOf(VadConfig.SAMPLE_RATE.toLong()))
        val stateTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(state),
            longArrayOf(2, 1, 128)
        )

        val inputs = mapOf(
            "input" to inputTensor,
            "sr" to srTensor,
            "state" to stateTensor
        )

        val results = session.run(inputs)

        val outputProb = (results[0].value as Array<FloatArray>)[0][0]

        val stateN = results[1].value as Array<Array<FloatArray>>
        for (i in 0 until 2) {
            for (j in 0 until 128) {
                state[i * 128 + j] = stateN[i][0][j]
            }
        }

        System.arraycopy(chunk, chunk.size - CONTEXT_SIZE, contextBuffer, 0, CONTEXT_SIZE)

        results.close()
        inputTensor.close()
        srTensor.close()
        stateTensor.close()

        updateState(outputProb)
        return outputProb
    }

    private fun updateState(probability: Float) {
        val now = System.currentTimeMillis()

        when (currentState) {
            VadState.SILENCE -> {
                if (probability >= config.speechThreshold) {
                    speechFrameCount = 1
                    currentState = VadState.TRANSITION
                }
            }
            VadState.TRANSITION -> {
                if (probability >= config.speechThreshold) {
                    speechFrameCount++
                    if (speechFrameCount >= config.speechMinFrames) {
                        currentState = VadState.SPEECH
                        speechStartTimeMs = now
                        silenceFrameCount = 0
                    }
                } else if (probability < config.silenceThreshold) {
                    silenceFrameCount++
                    if (silenceFrameCount >= config.silenceMinFrames) {
                        currentState = VadState.SILENCE
                        silenceStartTimeMs = now
                        speechFrameCount = 0
                    }
                } else {
                    speechFrameCount = 0
                    silenceFrameCount = 0
                    currentState = VadState.SILENCE
                }
            }
            VadState.SPEECH -> {
                if (probability < config.silenceThreshold) {
                    silenceFrameCount++
                    if (silenceFrameCount >= config.silenceMinFrames) {
                        currentState = VadState.SILENCE
                        silenceStartTimeMs = now
                        speechFrameCount = 0
                    }
                } else {
                    silenceFrameCount = 0
                }
            }
        }
    }

    override fun getCurrentState(): VadState = currentState

    override fun getSilenceDurationMs(): Long {
        if (currentState != VadState.SILENCE || silenceStartTimeMs == 0L) return 0L
        return System.currentTimeMillis() - silenceStartTimeMs
    }

    override fun getSpeechDurationMs(): Long {
        if (currentState != VadState.SPEECH || speechStartTimeMs == 0L) return 0L
        return System.currentTimeMillis() - speechStartTimeMs
    }

    override fun release() {
        ortSession?.close()
        ortEnv?.close()
        ortSession = null
        ortEnv = null
        isInitialized = false
    }

    private fun resetStates() {
        state.fill(0f)
        contextBuffer.fill(0f)
        currentState = VadState.SILENCE
        speechFrameCount = 0
        silenceFrameCount = 0
        silenceStartTimeMs = 0L
        speechStartTimeMs = 0L
    }

    companion object {
        private const val MODEL_FILE_NAME = "silero_vad.onnx"
        private const val CONTEXT_SIZE = 64
    }
}
