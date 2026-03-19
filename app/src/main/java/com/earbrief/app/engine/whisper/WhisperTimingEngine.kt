package com.earbrief.app.engine.whisper

import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerUrgency
import com.earbrief.app.domain.model.VadState
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class WhisperTimingEngine @Inject constructor(
    private val config: WhisperTimingConfig
) {
    private data class QueuedWhisper(
        val triggerEvent: TriggerEvent,
        val whisperText: String,
        val queuedAtMs: Long,
        val arrivalOrder: Long
    )

    private val queue = PriorityQueue<QueuedWhisper>(
        compareByDescending<QueuedWhisper> { it.triggerEvent.priority }
            .thenBy { it.arrivalOrder }
    )

    private val _outputCommands = MutableSharedFlow<WhisperOutputCommand>(extraBufferCapacity = 16)
    val outputCommands: SharedFlow<WhisperOutputCommand> = _outputCommands.asSharedFlow()

    private val _fadeOutSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val fadeOutSignal: SharedFlow<Unit> = _fadeOutSignal.asSharedFlow()

    private var arrivalCounter: Long = 0L
    private var isPlaying: Boolean = false
    private var nowMsProvider: () -> Long = { System.currentTimeMillis() }

    fun enqueue(event: TriggerEvent, whisperText: String) {
        val now = nowMsProvider()
        queue.add(
            QueuedWhisper(
                triggerEvent = event,
                whisperText = whisperText,
                queuedAtMs = now,
                arrivalOrder = arrivalCounter++
            )
        )

        if (!isPlaying && event.urgency == TriggerUrgency.CRITICAL) {
            emitReadyCommandWithTryEmit(vadState = VadState.SPEECH, silenceDurationMs = 0L)
        }
    }

    suspend fun onVadStateChanged(vadState: VadState, silenceDurationMs: Long) {
        pruneExpired(nowMsProvider())

        if (isPlaying && vadState == VadState.SPEECH) {
            _fadeOutSignal.emit(Unit)
            return
        }

        if (isPlaying) return

        emitReadyCommand(vadState = vadState, silenceDurationMs = silenceDurationMs)
    }

    fun onPlaybackStarted() {
        isPlaying = true
    }

    fun onPlaybackCompleted() {
        isPlaying = false
    }

    fun clear() {
        queue.clear()
        isPlaying = false
    }

    internal fun setNowProviderForTest(provider: () -> Long) {
        nowMsProvider = provider
    }

    private suspend fun emitReadyCommand(vadState: VadState, silenceDurationMs: Long) {
        val next = peekReadyItem(vadState, silenceDurationMs) ?: return
        queue.poll()
        _outputCommands.emit(next.toOutputCommand())
    }

    private fun emitReadyCommandWithTryEmit(vadState: VadState, silenceDurationMs: Long) {
        pruneExpired(nowMsProvider())
        val next = peekReadyItem(vadState, silenceDurationMs) ?: return
        queue.poll()
        _outputCommands.tryEmit(next.toOutputCommand())
    }

    private fun peekReadyItem(vadState: VadState, silenceDurationMs: Long): QueuedWhisper? {
        val candidate = queue.peek() ?: return null
        val now = nowMsProvider()
        val ageMs = now - candidate.queuedAtMs

        return when (candidate.triggerEvent.urgency) {
            TriggerUrgency.CRITICAL -> candidate
            TriggerUrgency.HIGH -> {
                val reachedSilence = vadState == VadState.SILENCE && silenceDurationMs >= config.highSilenceMs
                if (reachedSilence || ageMs >= config.highMaxWaitMs) candidate else null
            }
            TriggerUrgency.NORMAL -> {
                val reachedSilence = vadState == VadState.SILENCE && silenceDurationMs >= config.normalSilenceMs
                if (reachedSilence) candidate else null
            }
            TriggerUrgency.LOW -> {
                val reachedSilence = vadState == VadState.SILENCE && silenceDurationMs >= config.lowSilenceMs
                if (reachedSilence) candidate else null
            }
        }
    }

    private fun pruneExpired(nowMs: Long) {
        if (queue.isEmpty()) return

        val retained = queue.filter { queued ->
            when (queued.triggerEvent.urgency) {
                TriggerUrgency.NORMAL -> nowMs - queued.queuedAtMs < config.normalTimeoutMs
                TriggerUrgency.LOW -> nowMs - queued.queuedAtMs < config.lowTimeoutMs
                TriggerUrgency.HIGH,
                TriggerUrgency.CRITICAL -> true
            }
        }

        if (retained.size == queue.size) return

        queue.clear()
        queue.addAll(retained)
    }

    private fun QueuedWhisper.toOutputCommand(): WhisperOutputCommand {
        val urgency = triggerEvent.urgency
        return WhisperOutputCommand(
            triggerEvent = triggerEvent,
            whisperText = whisperText,
            volumeMultiplier = when (urgency) {
                TriggerUrgency.CRITICAL -> 0.8f
                TriggerUrgency.HIGH -> 0.7f
                TriggerUrgency.NORMAL -> 0.55f
                TriggerUrgency.LOW -> 0.4f
            },
            playVibration = urgency == TriggerUrgency.CRITICAL || urgency == TriggerUrgency.HIGH,
            vibrationDurationMs = when (urgency) {
                TriggerUrgency.CRITICAL -> 200L
                TriggerUrgency.HIGH -> 150L
                TriggerUrgency.NORMAL,
                TriggerUrgency.LOW -> 0L
            }
        )
    }
}
