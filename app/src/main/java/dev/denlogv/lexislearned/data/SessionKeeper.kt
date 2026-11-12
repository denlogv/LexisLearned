package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.PlannerSnapshot
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Remembers the unfinished study session of each scope: writes it to a [SessionStore] as the session goes on, off the calling
 * thread and in the order of the calls, and hands it back when the learner studies the same scope again.
 *
 * Saving is best effort: if the disk is full the session goes on, it just cannot be continued later. Writes complete even if the
 * caller was cancelled, so leaving the screen never leaves an outdated session behind.
 *
 * @param store where the sessions are kept.
 * @param clock the current time in epoch milliseconds; tests replace it.
 * @param dispatcher where the blocking calls run; tests replace it.
 */
class SessionKeeper(
    private val store: SessionStore,
    private val clock: () -> Long = System::currentTimeMillis,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // One thread, so a late "discard" can never overtake the "keep" that was called after it.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val io = dispatcher.limitedParallelism(1)

    /**
     * Remembers the state of a session, replacing the earlier one of the scope.
     *
     * @param scope what the session covers.
     * @param settings the settings the session was started with.
     * @param progress the counters shown on screen.
     * @param planner the plan of the session.
     */
    suspend fun keep(scope: SessionScope, settings: SessionSettings, progress: SessionProgress, planner: PlannerSnapshot) =
        guarded { store.save(SavedSession(scope, clock(), settings, progress, planner)) }

    /**
     * Finds the session left in a scope.
     *
     * @param scope what the session covers.
     * @return the session, or null if there is none or it is older than [MAX_AGE_MS]; an old one is forgotten.
     */
    suspend fun find(scope: SessionScope): SavedSession? = withContext(io) {
        val saved = store.load(scope)
        val usable = saved != null && saved.scope == scope && clock() - saved.savedAt <= MAX_AGE_MS
        if (saved != null && !usable) store.clear(scope)
        saved.takeIf { usable }
    }

    /**
     * Forgets the session of a scope: it is finished, or the learner does not want to continue it.
     *
     * @param scope what the session covers.
     */
    suspend fun discard(scope: SessionScope) = guarded { store.clear(scope) }

    /**
     * Runs a write on the keeper's thread, to completion, and ignores a disk error.
     *
     * @param write the write.
     */
    private suspend fun guarded(write: () -> Unit) = withContext(NonCancellable) {
        // Nested on purpose: a single withContext(io + NonCancellable) would still throw on its way back to a cancelled caller.
        withContext(io) {
            try {
                write()
            } catch (ignored: IOException) {
                // Best effort, see the class documentation.
            }
        }
    }

    /** Constants of the keeper. */
    companion object {
        /**
         * How long a session can be left before it is no longer offered: after this the learner has forgotten what the half-learned
         * words looked like, and a fresh session is the better start.
         */
        const val MAX_AGE_MS = 3 * 24 * 60 * 60_000L
    }
}
