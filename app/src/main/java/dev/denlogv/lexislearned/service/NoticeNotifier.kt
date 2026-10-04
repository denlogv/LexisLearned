package dev.denlogv.lexislearned.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.denlogv.lexislearned.MainActivity
import dev.denlogv.lexislearned.R

/**
 * Builds the notifications about deck generation: the ongoing one with a progress bar and a Pause button, and the one that says how
 * it ended.
 *
 * @param context the context the notifications are built for.
 */
class NoticeNotifier(private val context: Context) {
    init {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(PROGRESS_CHANNEL, "Deck generation", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel(RESULT_CHANNEL, "Deck results", NotificationManager.IMPORTANCE_DEFAULT))
    }

    /**
     * Builds a notification.
     *
     * @param notice what it says.
     * @return the notification; tapping it opens the app, and the Pause button pauses the generation.
     */
    fun build(notice: GenerationNotice): Notification {
        val builder = NotificationCompat.Builder(context, if (notice.ongoing) PROGRESS_CHANNEL else RESULT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notice.title)
            .setContentText(notice.text)
            .setContentIntent(openApp())
            .setOngoing(notice.ongoing)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        notice.progressPercent?.let { builder.setProgress(PERCENT, it, false) }
        notice.pauseLabel?.let { builder.addAction(0, it, pause()) }
        return builder.build()
    }

    /**
     * The intent that opens the app.
     *
     * @return the pending intent.
     */
    private fun openApp(): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    /**
     * The intent that tells the service to pause the generation.
     *
     * @return the pending intent.
     */
    private fun pause(): PendingIntent = PendingIntent.getService(
        context,
        1,
        Intent(context, GenerationService::class.java).setAction(GenerationService.ACTION_PAUSE),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val PROGRESS_CHANNEL = "generation"
        const val RESULT_CHANNEL = "generation_result"
        const val PERCENT = 100
    }
}
