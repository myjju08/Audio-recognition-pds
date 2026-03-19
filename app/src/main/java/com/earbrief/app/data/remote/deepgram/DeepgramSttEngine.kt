package com.earbrief.app.data.remote.deepgram

import com.earbrief.app.BuildConfig
import com.earbrief.app.data.remote.deepgram.model.DeepgramResponse
import com.earbrief.app.di.IoDispatcher
import com.earbrief.app.engine.stt.SttConnectionState
import com.earbrief.app.engine.stt.SttEngine
import com.earbrief.app.engine.stt.model.TranscriptResult
import com.earbrief.app.engine.stt.model.TranscriptWord
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepgramSttEngine @Inject constructor(
    private val httpClient: HttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SttEngine {

    private val json = Json { ignoreUnknownKeys = true }

    private var session: WebSocketSession? = null
    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null
    private var scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private var reconnectAttempt = 0
    private var shouldReconnect = false

    private val _connectionState = MutableStateFlow(SttConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<SttConnectionState> = _connectionState.asStateFlow()

    private val _transcriptResults = MutableSharedFlow<TranscriptResult>(extraBufferCapacity = 64)
    override val transcriptResults: SharedFlow<TranscriptResult> = _transcriptResults.asSharedFlow()

    override suspend fun connect() {
        if (_connectionState.value == SttConnectionState.CONNECTED) return
        shouldReconnect = true
        reconnectAttempt = 0
        doConnect()
    }

    private suspend fun doConnect() {
        _connectionState.value = SttConnectionState.CONNECTING
        try {
            val url = DeepgramConfig.buildWsUrl()
            session = httpClient.webSocketSession(url) {
                headers.append("Authorization", "Token ${BuildConfig.DEEPGRAM_API_KEY}")
            }

            _connectionState.value = SttConnectionState.CONNECTED
            reconnectAttempt = 0
            startReceiveLoop()
            startHeartbeat()
        } catch (e: Exception) {
            handleConnectionFailure(e)
        }
    }

    private fun startReceiveLoop() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            val ws = session ?: return@launch
            try {
                for (frame in ws.incoming) {
                    when (frame) {
                        is Frame.Text -> handleTextFrame(frame.readText())
                        is Frame.Close -> {
                            handleDisconnect()
                            return@launch
                        }
                        else -> { }
                    }
                }
            } catch (e: Exception) {
                if (isActive) handleDisconnect()
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value == SttConnectionState.CONNECTED) {
                delay(DeepgramConfig.HEARTBEAT_INTERVAL_MS)
                try {
                    session?.send(Frame.Text("{\"type\": \"KeepAlive\"}"))
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun handleTextFrame(text: String) {
        try {
            val response = json.decodeFromString<DeepgramResponse>(text)
            if (response.type != "Results") return

            val alt = response.channel?.alternatives?.firstOrNull() ?: return
            if (alt.transcript.isBlank()) return

            val result = TranscriptResult(
                transcript = alt.transcript,
                confidence = alt.confidence,
                isFinal = response.isFinal,
                speechFinal = response.speechFinal,
                words = alt.words.map { w ->
                    TranscriptWord(
                        word = w.word,
                        punctuatedWord = w.punctuatedWord.ifBlank { w.word },
                        startSec = w.start,
                        endSec = w.end,
                        confidence = w.confidence
                    )
                },
                durationMs = (response.duration * 1000).toLong()
            )
            _transcriptResults.tryEmit(result)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Deepgram response", e)
        }
    }

    private suspend fun handleDisconnect() {
        heartbeatJob?.cancel()
        session = null

        if (!shouldReconnect) {
            _connectionState.value = SttConnectionState.DISCONNECTED
            return
        }

        if (reconnectAttempt < DeepgramConfig.MAX_RECONNECT_ATTEMPTS) {
            _connectionState.value = SttConnectionState.RECONNECTING
            val delayMs = DeepgramConfig.RECONNECT_DELAYS.getOrElse(reconnectAttempt) { 16000L }
            reconnectAttempt++
            delay(delayMs)
            doConnect()
        } else {
            _connectionState.value = SttConnectionState.FAILED
        }
    }

    private suspend fun handleConnectionFailure(error: Exception) {
        Log.w(TAG, "Deepgram connection failed", error)
        if (shouldReconnect && reconnectAttempt < DeepgramConfig.MAX_RECONNECT_ATTEMPTS) {
            _connectionState.value = SttConnectionState.RECONNECTING
            val delayMs = DeepgramConfig.RECONNECT_DELAYS.getOrElse(reconnectAttempt) { 16000L }
            reconnectAttempt++
            delay(delayMs)
            doConnect()
        } else {
            _connectionState.value = SttConnectionState.FAILED
        }
    }

    override suspend fun sendAudioFrame(pcmData: ShortArray) {
        val ws = session ?: return
        if (_connectionState.value != SttConnectionState.CONNECTED) return

        val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcmData) {
            byteBuffer.putShort(sample)
        }
        try {
            ws.send(Frame.Binary(true, byteBuffer.array()))
        } catch (_: Exception) {
            handleDisconnect()
        }
    }

    override suspend fun endOfStream() {
        try {
            session?.send(Frame.Text("{\"type\": \"CloseStream\"}"))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send end-of-stream", e)
        }
    }

    override suspend fun disconnect() {
        shouldReconnect = false
        heartbeatJob?.cancel()
        receiveJob?.cancel()
        try {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "User disconnect"))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close Deepgram session", e)
        }
        session = null
        _connectionState.value = SttConnectionState.DISCONNECTED
    }

    override fun release() {
        shouldReconnect = false
        heartbeatJob?.cancel()
        receiveJob?.cancel()
        try {
            session?.let { currentSession ->
                scope.launch {
                    runCatching { currentSession.close() }
                        .onFailure { closeError ->
                            Log.w(TAG, "Failed to close Deepgram session on release", closeError)
                        }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule Deepgram close on release", e)
        }
        session = null
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        _connectionState.value = SttConnectionState.DISCONNECTED
    }

    private companion object {
        const val TAG = "DeepgramSttEngine"
    }
}
