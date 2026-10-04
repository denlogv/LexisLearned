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
     * @return the notification; tapping it opens the app, and its buttons pause the generation after the section or at once.
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
        notice.pauseAfterSectionLabel?.let { builder.addAction(0, it, pause(PauseRequest.AFTER_SECTION)) }
        notice.pauseNowLabel?.let { builder.addAction(0, it, pause(PauseRequest.NOW)) }
        return builder.build()
    }

    /**
     * Shows how a generation ended. It is a notification of its own, not an update of the progress one: the system posts the progress
     * notification of a foreground service on its own schedule, and a result sent under the same id right after it could be overtaken
     * and replaced by the stale progress.
     *
     * @param notice how the generation ended.
     */
    fun showResult(notice: GenerationNotice) = manager().notify(RESULT_ID, build(notice))

    /** Takes the notification about how an earlier generation ended away, for example when the next one starts. */
    fun cancelResult() = manager().cancel(RESULT_ID)

    /** Takes both notifications away, whether or not the service that posted them still runs. */
    fun cancel() {
        cancelResult()
        manager().cancel(PROGRESS_ID)
    }

    /**
     * The system's notification service.
     *
     * @return the manager.
     */
    private fun manager(): NotificationManager = context.getSystemService(NotificationManager::class.java)

    /**
     * The intent that opens the app.
     *
     * @return the pending intent.
     */
    private fun openApp(): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    /**
     * The intent that tells the service to pause the generation. Each kind of pause has a request code of its own, or the system would
     * hand both buttons the same pending intent.
     *
     * @param request which pause the button asks for.
     * @return the pending intent.
     */
    private fun pause(request: PauseRequest): PendingIntent = PendingIntent.getService(
        context,
        request.ordinal,
        Intent(context, GenerationService::class.java).setAction(request.action),
        PendingIntent.FLAG_IMMUTABLE,
    )

    /** The ids of the two notifications. */
    companion object {
        /** The id of the progress notification, which belongs to the foreground service. */
        const val PROGRESS_ID = 1

        /** The id of the notification that says how a generation ended. */
        const val RESULT_ID = 2

        private const val PROGRESS_CHANNEL = "generation"
        private const val RESULT_CHANNEL = "generation_result"
        private const val PERCENT = 100
    }
}
