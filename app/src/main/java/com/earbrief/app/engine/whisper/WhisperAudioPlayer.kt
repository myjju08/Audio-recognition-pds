package com.earbrief.app.engine.whisper

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import com.earbrief.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class WhisperAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val operationMutex = Mutex()
    private val audioManager: AudioManager by lazy { context.getSystemService(AudioManager::class.java) }
    private val vibrator: Vibrator? by lazy { context.getSystemService(Vibrator::class.java) }

    private var mediaPlayer: MediaPlayer? = null
    private var playing = false
    private var originalStreamVolume: Int? = null
    private var tempAudioFile: File? = null
    private var playerVolume: Float = 1f

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    suspend fun play(
        audioBytes: ByteArray,
        volumeMultiplier: Float = 0.5f,
        vibrateMs: Long = 0
    ): Result<Unit> = withContext(ioDispatcher) {
        operationMutex.withLock {
            runCatching {
                stopLocked()

                val tempFile = writeTempMp3File(audioBytes)
                val mp = MediaPlayer()
                configureAudioRouting(mp)
                applyVolumeMultiplier(volumeMultiplier)

                if (vibrateMs > 0) {
                    triggerVibration(vibrateMs)
                }

                mp.setOnCompletionListener {
                    scope.launch {
                        operationMutex.withLock {
                            stopLocked()
                        }
                    }
                }
                mp.setOnErrorListener { _, _, _ ->
                    scope.launch {
                        operationMutex.withLock {
                            stopLocked()
                        }
                    }
                    true
                }

                FileInputStream(tempFile).use { inputStream ->
                    mp.setDataSource(inputStream.fd)
                }
                mp.prepare()
                mp.start()

                mediaPlayer = mp
                tempAudioFile = tempFile
                playerVolume = 1f
                playing = true
                _playbackState.value = PlaybackState.PLAYING
            }.onFailure {
                stopLocked()
            }
        }
    }

    suspend fun fadeOut(durationMs: Long = 300) {
        withContext(ioDispatcher) {
            operationMutex.withLock {
                val player = mediaPlayer ?: return@withContext
                if (!playing) {
                    return@withContext
                }

                _playbackState.value = PlaybackState.FADING_OUT

                val rampDuration = durationMs.coerceAtLeast(1)
                val steps = 10
                val stepDelay = (rampDuration / steps).coerceAtLeast(1)
                val startVolume = playerVolume.coerceIn(0f, 1f)

                for (step in steps downTo 1) {
                    val nextVolume = startVolume * (step - 1) / steps
                    try {
                        player.setVolume(nextVolume, nextVolume)
                    } catch (_: IllegalStateException) {
                        break
                    }
                    playerVolume = nextVolume
                    delay(stepDelay)
                }

                stopLocked()
            }
        }
    }

    fun stop() {
        runBlocking(ioDispatcher) {
            operationMutex.withLock {
                stopLocked()
            }
        }
    }

    fun release() {
        runBlocking(ioDispatcher) {
            operationMutex.withLock {
                stopLocked()
            }
        }
        scope.cancel()
    }

    fun isPlaying(): Boolean = playing

    private fun configureAudioRouting(player: MediaPlayer) {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )

        val bluetoothDevice = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        if (bluetoothDevice != null) {
            player.setPreferredDevice(bluetoothDevice)
        }
    }

    private fun applyVolumeMultiplier(volumeMultiplier: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val clampedMultiplier = volumeMultiplier.coerceIn(0f, 1f)
        val targetVolume = (currentVolume * clampedMultiplier)
            .roundToInt()
            .coerceIn(0, maxVolume)

        originalStreamVolume = currentVolume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
    }

    private fun triggerVibration(vibrateMs: Long) {
        val duration = vibrateMs.coerceAtLeast(1)
        val vibratorService = vibrator ?: return
        if (!vibratorService.hasVibrator()) {
            return
        }

        vibratorService.vibrate(
            VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    private fun writeTempMp3File(audioBytes: ByteArray): File {
        val file = File.createTempFile("whisper_", ".mp3", context.cacheDir)
        file.outputStream().use { output ->
            output.write(audioBytes)
            output.flush()
        }
        return file
    }

    private fun stopLocked() {
        val player = mediaPlayer
        mediaPlayer = null

        if (player != null) {
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: IllegalStateException) {
            }
            player.release()
        }

        originalStreamVolume?.let { originalVolume ->
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
        }
        originalStreamVolume = null

        tempAudioFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        tempAudioFile = null

        playerVolume = 1f
        playing = false
        _playbackState.value = PlaybackState.IDLE
    }
}

enum class PlaybackState {
    IDLE,
    PLAYING,
    FADING_OUT
}
