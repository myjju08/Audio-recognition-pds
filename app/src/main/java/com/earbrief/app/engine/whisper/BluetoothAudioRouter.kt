package com.earbrief.app.engine.whisper

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothAudioRouter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager by lazy {
        context.getSystemService(AudioManager::class.java)
    }

    fun isBluetoothAudioConnected(): Boolean {
        return getConnectedBluetoothA2dpDevice() != null
    }

    fun getConnectedDeviceName(): String? {
        return getConnectedBluetoothA2dpDevice()?.productName?.toString()
    }

    private fun getConnectedBluetoothA2dpDevice(): AudioDeviceInfo? {
        return audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
    }
}
