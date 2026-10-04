package dev.denlogv.lexislearned.service

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ai.GenerationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Follows the generation for the service that keeps the app alive: it shows the progress while the generation runs, and says how it
 * ended and lets the service stop when it does not run any more.
 *
 * @param generation the generation to follow and to pause.
 * @param scope where to follow it.
 * @param host the service.
 */
class GenerationWatcher(private val generation: GenerationManager, private val scope: CoroutineScope, private val host: Host) {
    /** What the watcher needs from the service. */
    interface Host {
        /**
         * Shows the progress of a running generation; the service is kept in the foreground.
         *
         * @param notice what to show.
         */
        fun show(notice: GenerationNotice)

        /**
         * Ends the foreground state: shows how the generation ended and stops the service.
         *
         * @param notice how it ended, or null if there is nothing to tell.
         */
        fun finish(notice: GenerationNotice?)
    }

    private var watching: Job? = null

    /**
     * Starts following the generation, unless it is followed already.
     *
     * @param pause the pause the user asked for with a button of the notification, if any.
     */
    fun start(pause: PauseRequest) {
        when (pause) {
            PauseRequest.AFTER_SECTION -> generation.pauseAfterSection()
            PauseRequest.NOW -> generation.pauseNow()
            PauseRequest.NONE -> Unit
        }
        if (watching == null) watching = scope.launch { generation.state.collect(::react) }
    }

    /** Stops following the generation. */
    fun stop() {
        watching?.cancel()
        watching = null
    }

    /**
     * Passes a state on to the service.
     *
     * @param state the state of the generation.
     */
    private fun react(state: GenState) {
        val notice = GenerationNotice.of(state)
        if (state is GenState.Running && notice != null) host.show(notice) else host.finish(notice)
    }
}
