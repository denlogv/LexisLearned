package dev.denlogv.lexislearned.data

import java.io.IOException

/**
 * Keeps the unfinished study session of each scope so that it can be continued after the screen was left or the app was killed.
 * Calls block; [SessionKeeper] runs them off the main thread and treats a failure as "not saved".
 */
interface SessionStore {
    /**
     * Stores a session, replacing an earlier one of the same scope. It is never half written.
     *
     * @param session the session.
     * @throws IOException if it cannot be written.
     */
    fun save(session: SavedSession)

    /**
     * Reads the session of a scope.
     *
     * @param scope what the session covers.
     * @return the session, or null if there is none or what is stored cannot be read.
     */
    fun load(scope: SessionScope): SavedSession?

    /**
     * Forgets the session of a scope; nothing happens if there is none.
     *
     * @param scope what the session covers.
     */
    fun clear(scope: SessionScope)
}
