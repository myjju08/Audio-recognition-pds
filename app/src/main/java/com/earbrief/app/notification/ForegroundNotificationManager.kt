package com.earbrief.app.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.earbrief.app.R
import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.presentation.MainActivity
import com.earbrief.app.service.AudioCaptureService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val NOTIFICATION_ID = 1001
    }

    fun buildNotification(state: ListeningState): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.LISTENING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        when (state) {
            ListeningState.LISTENING -> {
                builder
                    .setContentTitle(context.getString(R.string.notification_listening_title))
                    .setContentText(context.getString(R.string.notification_listening_body))
                    .addAction(buildPauseAction())
                    .addAction(buildStopAction())
            }
            ListeningState.PAUSED -> {
                builder
                    .setContentTitle(context.getString(R.string.notification_paused_title))
                    .setContentText(context.getString(R.string.notification_paused_body))
                    .addAction(buildResumeAction())
                    .addAction(buildStopAction())
            }
            else -> {
                builder
                    .setContentTitle(context.getString(R.string.notification_paused_title))
                    .setContentText(context.getString(R.string.notification_paused_body))
            }
        }

        return builder.build()
    }

    private fun buildPauseAction(): NotificationCompat.Action {
        val intent = Intent(context, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_PAUSE
        }
        val pendingIntent = PendingIntent.getService(
            context, 1, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            context.getString(R.string.action_pause),
            pendingIntent
        ).build()
    }

    private fun buildResumeAction(): NotificationCompat.Action {
        val intent = Intent(context, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_RESUME
        }
        val pendingIntent = PendingIntent.getService(
            context, 2, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            context.getString(R.string.action_resume),
            pendingIntent
        ).build()
    }

    private fun buildStopAction(): NotificationCompat.Action {
        val intent = Intent(context, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_STOP
        }
        val pendingIntent = PendingIntent.getService(
            context, 3, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            context.getString(R.string.action_stop),
            pendingIntent
        ).build()
    }
}
