package com.earbrief.app.engine.whisper

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BluetoothAudioRouter")
class BluetoothAudioRouterTest {

    @Test
    fun `returns true when A2DP device is connected`() {
        val context = mockk<Context>()
        val audioManager = mockk<AudioManager>()
        val btDevice = mockk<AudioDeviceInfo>()
        val wiredDevice = mockk<AudioDeviceInfo>()

        every { context.getSystemService(AudioManager::class.java) } returns audioManager
        every { btDevice.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        every { btDevice.productName } returns "Galaxy Buds"
        every { wiredDevice.type } returns AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(wiredDevice, btDevice)

        val router = BluetoothAudioRouter(context)

        assertTrue(router.isBluetoothAudioConnected())
        assertEquals("Galaxy Buds", router.getConnectedDeviceName())
    }

    @Test
    fun `returns false when no A2DP device is connected`() {
        val context = mockk<Context>()
        val audioManager = mockk<AudioManager>()
        val speaker = mockk<AudioDeviceInfo>()

        every { context.getSystemService(AudioManager::class.java) } returns audioManager
        every { speaker.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(speaker)

        val router = BluetoothAudioRouter(context)

        assertFalse(router.isBluetoothAudioConnected())
        assertEquals(null, router.getConnectedDeviceName())
    }
}
