package com.earbrief.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.earbrief.app.di.IoDispatcher
import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.engine.context.ContextEngine
import com.earbrief.app.engine.stt.SttStreamBridge
import com.earbrief.app.engine.vad.VadConfig
import com.earbrief.app.engine.vad.VadEngine
import com.earbrief.app.engine.whisper.WhisperPipelineCoordinator
import com.earbrief.app.notification.ForegroundNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AudioCaptureService : Service() {

    @Inject lateinit var vadEngine: VadEngine
    @Inject lateinit var sttStreamBridge: SttStreamBridge
    @Inject lateinit var contextEngine: ContextEngine
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var pipelineOrchestrator: PipelineOrchestrator
    @Inject lateinit var whisperPipelineCoordinator: WhisperPipelineCoordinator
    @Inject lateinit var notificationManager: ForegroundNotificationManager
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    private var audioRecord: AudioRecord? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var captureJob: Job? = null

    private lateinit var serviceScope: CoroutineScope

    private val _listeningState = MutableStateFlow(ListeningState.IDLE)
    val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

    private val _vadState = MutableStateFlow(VadState.SILENCE)
    val vadState: StateFlow<VadState> = _vadState.asStateFlow()

    private val _speechProbability = MutableSharedFlow<Float>(extraBufferCapacity = 64)
    val speechProbability: SharedFlow<Float> = _speechProbability.asSharedFlow()

    private val _audioFrames = MutableSharedFlow<ShortArray>(extraBufferCapacity = 32)
    val audioFrames: SharedFlow<ShortArray> = _audioFrames.asSharedFlow()

    private val framePool = ArrayDeque<ShortArray>(POOL_SIZE)

    inner class LocalBinder : Binder() {
        fun getService(): AudioCaptureService = this@AudioCaptureService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + ioDispatcher)
        pipelineOrchestrator.updateSessionId(sessionManager.sessionId.value)
        whisperPipelineCoordinator.start(serviceScope)

        serviceScope.launch {
            sessionManager.sessionId.collect { id ->
                pipelineOrchestrator.updateSessionId(id)
            }
        }

        serviceScope.launch {
            vadEngine.initialize(this@AudioCaptureService)
        }

        sttStreamBridge.start()

        serviceScope.launch {
            sttStreamBridge.transcripts.collect { result ->
                pipelineOrchestrator.updateInterimTranscript(
                    if (result.isFinal) "" else result.transcript
                )
                if (result.isFinal) {
                    pipelineOrchestrator.appendFinalTranscript(result.transcript)
                }
                contextEngine.onTranscript(result)
            }
        }

        serviceScope.launch {
            contextEngine.utteranceFlow.collect { utterance ->
                pipelineOrchestrator.publishUtterance(utterance)
            }
        }

        serviceScope.launch {
            sttStreamBridge.sttConnectionState.collect { state ->
                pipelineOrchestrator.updateSttConnectionState(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startListening()
            ACTION_PAUSE -> pauseListening()
            ACTION_RESUME -> resumeListening()
            ACTION_STOP -> stopListening()
        }
        return START_STICKY
    }

    private fun startListening() {
        if (_listeningState.value == ListeningState.LISTENING) return
        whisperPipelineCoordinator.start(serviceScope)

        startForeground(
            ForegroundNotificationManager.NOTIFICATION_ID,
            notificationManager.buildNotification(ListeningState.LISTENING),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        acquireWakeLock()
        initAudioRecord()
        startCapture()
        _listeningState.value = ListeningState.LISTENING
        pipelineOrchestrator.updateListeningState(ListeningState.LISTENING)
        serviceScope.launch {
            sessionManager.onListeningStateChanged(ListeningState.LISTENING)
        }

        isRunning = true
    }

    private fun pauseListening() {
        captureJob?.cancel()
        captureJob = null
        audioRecord?.stop()
        _listeningState.value = ListeningState.PAUSED
        pipelineOrchestrator.updateListeningState(ListeningState.PAUSED)
        serviceScope.launch {
            contextEngine.onListeningStateChanged(ListeningState.PAUSED)
        }

        val notification = notificationManager.buildNotification(ListeningState.PAUSED)
        val nm = getSystemService<android.app.NotificationManager>()
        nm?.notify(ForegroundNotificationManager.NOTIFICATION_ID, notification)
    }

    private fun resumeListening() {
        if (_listeningState.value != ListeningState.PAUSED) return
        audioRecord?.startRecording()
        startCapture()
        _listeningState.value = ListeningState.LISTENING
        pipelineOrchestrator.updateListeningState(ListeningState.LISTENING)
        serviceScope.launch {
            contextEngine.onListeningStateChanged(ListeningState.LISTENING)
        }

        val notification = notificationManager.buildNotification(ListeningState.LISTENING)
        val nm = getSystemService<android.app.NotificationManager>()
        nm?.notify(ForegroundNotificationManager.NOTIFICATION_ID, notification)
    }

    private fun stopListening() {
        _listeningState.value = ListeningState.STOPPING
        captureJob?.cancel()
        captureJob = null
        releaseAudioRecord()
        releaseWakeLock()
        _listeningState.value = ListeningState.IDLE
        pipelineOrchestrator.updateListeningState(ListeningState.IDLE)
        serviceScope.launch {
            sessionManager.onListeningStateChanged(ListeningState.IDLE)
            contextEngine.onListeningStateChanged(ListeningState.IDLE)
        }
        pipelineOrchestrator.clearTranscripts()
        whisperPipelineCoordinator.stop()
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun initAudioRecord() {
        val minBufSize = AudioRecord.getMinBufferSize(
            VadConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            VadConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufSize, VadConfig.FRAME_SIZE * 2)
        )

        audioRecord?.startRecording()
    }

    private fun startCapture() {
        captureJob = serviceScope.launch {
            val recorder = audioRecord ?: return@launch

            while (isActive) {
                val frame = acquireFrame()
                val readCount = recorder.read(frame, 0, VadConfig.FRAME_SIZE)

                if (readCount == VadConfig.FRAME_SIZE) {
                    val probability = vadEngine.processFrame(frame)
                    _speechProbability.tryEmit(probability)
                    val currentVadState = vadEngine.getCurrentState()
                    val silenceDurationMs = vadEngine.getSilenceDurationMs()
                    _vadState.value = currentVadState
                    pipelineOrchestrator.updateSpeechProbability(probability)
                    pipelineOrchestrator.updateVadState(currentVadState)

                    whisperPipelineCoordinator.onVadStateChanged(
                        vadState = currentVadState,
                        silenceDurationMs = silenceDurationMs
                    )

                    sttStreamBridge.onVadStateChanged(currentVadState)

                    if (currentVadState == VadState.SPEECH) {
                        _audioFrames.tryEmit(frame.copyOf())
                        sttStreamBridge.sendAudioFrame(frame.copyOf())
                    }

                    zeroAndRelease(frame)
                } else {
                    zeroAndRelease(frame)
                }
            }
        }
    }

    private fun acquireFrame(): ShortArray {
        return synchronized(framePool) {
            framePool.removeLastOrNull() ?: ShortArray(VadConfig.FRAME_SIZE)
        }
    }

    private fun zeroAndRelease(frame: ShortArray) {
        frame.fill(0)
        synchronized(framePool) {
            if (framePool.size < POOL_SIZE) {
                framePool.addLast(frame)
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService<PowerManager>() ?: return
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EarBrief::AudioCapture"
        ).apply {
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun releaseAudioRecord() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    override fun onDestroy() {
        captureJob?.cancel()
        releaseAudioRecord()
        releaseWakeLock()
        sttStreamBridge.stop()
        whisperPipelineCoordinator.stop()
        vadEngine.release()
        serviceScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.earbrief.action.START"
        const val ACTION_PAUSE = "com.earbrief.action.PAUSE"
        const val ACTION_RESUME = "com.earbrief.action.RESUME"
        const val ACTION_STOP = "com.earbrief.action.STOP"

        private const val POOL_SIZE = 16
        private const val WAKE_LOCK_TIMEOUT_MS = 4 * 60 * 60 * 1000L

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, AudioCaptureService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, AudioCaptureService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, AudioCaptureService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
