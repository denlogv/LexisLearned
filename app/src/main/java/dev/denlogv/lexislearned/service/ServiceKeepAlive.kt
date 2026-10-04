package dev.denlogv.lexislearned.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dev.denlogv.lexislearned.ai.KeepAlive

/**
 * Keeps the process alive by starting [GenerationService], which runs in the foreground while a generation does.
 *
 * @param context the application context.
 */
class ServiceKeepAlive(private val context: Context) : KeepAlive {
    /** Starts the service. If the system does not allow that, the generation simply runs without it: it is still resumable. */
    override fun hold() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, GenerationService::class.java))
        } catch (ignored: IllegalStateException) {
            // Started while the app is in the background, which the system forbids.
        } catch (ignored: SecurityException) {
            // The permission for a foreground service of this type was withdrawn.
        }
    }
}
