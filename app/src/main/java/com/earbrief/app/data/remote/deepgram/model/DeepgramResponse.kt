package com.earbrief.app.data.remote.deepgram.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepgramResponse(
    val type: String = "",
    val channel: DeepgramChannel? = null,
    @SerialName("is_final") val isFinal: Boolean = false,
    @SerialName("speech_final") val speechFinal: Boolean = false,
    val duration: Float = 0f,
    val start: Float = 0f
)

@Serializable
data class DeepgramChannel(
    val alternatives: List<DeepgramAlternative> = emptyList()
)

@Serializable
data class DeepgramAlternative(
    val transcript: String = "",
    val confidence: Float = 0f,
    val words: List<DeepgramWord> = emptyList()
)

@Serializable
data class DeepgramWord(
    val word: String = "",
    val start: Float = 0f,
    val end: Float = 0f,
    val confidence: Float = 0f,
    @SerialName("punctuated_word") val punctuatedWord: String = ""
)
