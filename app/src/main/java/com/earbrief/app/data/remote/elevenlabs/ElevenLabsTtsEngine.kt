package com.earbrief.app.data.remote.elevenlabs

import com.earbrief.app.BuildConfig
import com.earbrief.app.engine.tts.TtsConfig
import com.earbrief.app.engine.tts.TtsEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ElevenLabsTtsEngine @Inject constructor(
    private val httpClient: HttpClient,
    private val config: TtsConfig
) : TtsEngine {

    override suspend fun synthesize(text: String): Result<ByteArray> {
        if (BuildConfig.ELEVENLABS_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("ELEVENLABS_API_KEY is missing"))
        }

        return runCatching {
            val response = httpClient.post("https://api.elevenlabs.io/v1/text-to-speech/${config.voiceId}") {
                url {
                    parameters.append("output_format", config.outputFormat)
                }
                contentType(ContentType.Application.Json)
                accept(ContentType.Audio.MPEG)
                header("xi-api-key", BuildConfig.ELEVENLABS_API_KEY)
                setBody(
                    SynthesisRequest(
                        text = text,
                        modelId = config.modelId,
                        voiceSettings = VoiceSettings(
                            stability = config.stability,
                            similarityBoost = config.similarityBoost,
                            style = config.style,
                            speed = config.speakingRate
                        )
                    )
                )
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.body<String>()
                throw IllegalStateException("ElevenLabs TTS failed: ${response.status.value} $errorBody")
            }

            response.body<ByteArray>()
        }
    }

    override fun release() = Unit

    @Serializable
    private data class SynthesisRequest(
        val text: String,
        @SerialName("model_id")
        val modelId: String,
        @SerialName("voice_settings")
        val voiceSettings: VoiceSettings
    )

    @Serializable
    private data class VoiceSettings(
        val stability: Float,
        @SerialName("similarity_boost")
        val similarityBoost: Float,
        val style: Float,
        val speed: Float
    )
}
