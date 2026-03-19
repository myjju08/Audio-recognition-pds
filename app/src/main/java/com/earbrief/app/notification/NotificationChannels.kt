package com.earbrief.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

object NotificationChannels {

    const val LISTENING_CHANNEL_ID = "earbrief_listening"
    const val WHISPER_CHANNEL_ID = "earbrief_whisper"

    fun createAll(context: Context) {
        val notificationManager = context.getSystemService<NotificationManager>() ?: return

        val listeningChannel = NotificationChannel(
            LISTENING_CHANNEL_ID,
            "청취 상태",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "귀띔 청취 서비스 알림"
            setShowBadge(false)
        }

        val whisperChannel = NotificationChannel(
            WHISPER_CHANNEL_ID,
            "귓속말 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "귓속말 히스토리 알림"
        }

        notificationManager.createNotificationChannels(
            listOf(listeningChannel, whisperChannel)
        )
    }
}
