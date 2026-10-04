package dev.denlogv.lexislearned.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import dev.denlogv.lexislearned.LexisLearnedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Keeps the app in the foreground while a deck is generated, so that Android does not kill the process when the user switches to
 * another app or locks the screen, and shows the progress in a notification. It stops itself when the generation is not running any
 * more. What it does is decided by [GenerationWatcher]; this class only talks to Android.
 */
class GenerationService :
    Service(),
    GenerationWatcher.Host {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notifier by lazy { NoticeNotifier(this) }
    private var watcher: GenerationWatcher? = null
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Not a bound service.
     *
     * @param intent the intent the service was bound with.
     * @return null.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Enters the foreground at once, as the system requires, and starts following the generation.
     *
     * @param intent the start request; its action tells whether the Pause button of the notification was pressed.
     * @param flags how the request was delivered.
     * @param startId the request's id.
     * @return that the service is not restarted if the system kills it: the paused generation is restored when the app opens.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        show(GenerationNotice.STARTING)
        holdWakeLock()
        val following = watcher ?: GenerationWatcher((application as LexisLearnedApp).generation, scope, this).also { watcher = it }
        following.start(pause = intent?.action == ACTION_PAUSE)
        return START_NOT_STICKY
    }

    /**
     * Shows the progress and stays in the foreground.
     *
     * @param notice what to show.
     */
    override fun show(notice: GenerationNotice) {
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notifier.build(notice), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    /**
     * Leaves the foreground, keeping a notification that says how the generation ended, and stops.
     *
     * @param notice how it ended, or null if there is nothing to tell.
     */
    override fun finish(notice: GenerationNotice?) {
        if (notice == null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notifier.build(notice))
        }
        stopSelf()
    }

    /**
     * Android's limit for this kind of service was reached (a few hours a day): pauses the generation, which is kept and can be
     * resumed, and stops. The system raises an error if the service does not stop soon after this call.
     *
     * @param startId the id of the request the service was started with.
     * @param fgsType the type of the foreground service that timed out.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        (application as LexisLearnedApp).generation.pause()
        stopSelf()
    }

    /** Stops following the generation and lets the device sleep again. */
    override fun onDestroy() {
        watcher?.stop()
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    /** Keeps the processor awake, so that a generation goes on with the screen off; the lock expires by itself as a safeguard. */
    private fun holdWakeLock() {
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LexisLearned:generation")
                .apply { setReferenceCounted(false) }
        }
        wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    /** What the pause button of the notification sends, and the notification's id. */
    companion object {
        /** The action of the intent that pauses the generation. */
        const val ACTION_PAUSE = "dev.denlogv.lexislearned.PAUSE"

        private const val NOTIFICATION_ID = 1
        private const val WAKE_LOCK_TIMEOUT_MS = 3 * 60 * 60 * 1000L
    }
}
