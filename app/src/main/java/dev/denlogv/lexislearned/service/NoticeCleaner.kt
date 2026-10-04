package dev.denlogv.lexislearned.service

import dev.denlogv.lexislearned.ai.GenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Takes the notification about deck generation away when the generation it describes is gone. The service that posts the notification
 * stops as soon as the generation is not running, and leaves how it ended (paused, finished, failed) behind; nobody else would remove
 * that when the user then discards the rest or dismisses the result.
 *
 * It only reacts to a change from a state that has something to tell to one that has not, so a notification left by an earlier run of
 * the app survives until the paused generation is restored, and one that the service replaces is not touched.
 *
 * @param state the state of the generation to follow.
 * @param scope where to follow it.
 * @param cancel removes the notification.
 */
class NoticeCleaner(private val state: StateFlow<GenState>, private val scope: CoroutineScope, private val cancel: () -> Unit) {
    private var watching: Job? = null

    /** Starts following the state, unless it is followed already. */
    fun start() {
        if (watching != null) return
        watching = scope.launch {
            var shown = false
            state.collect { current ->
                val tells = GenerationNotice.of(current) != null
                if (shown && !tells) cancel()
                shown = tells
            }
        }
    }

    /** Stops following the state. */
    fun stop() {
        watching?.cancel()
        watching = null
    }
}
