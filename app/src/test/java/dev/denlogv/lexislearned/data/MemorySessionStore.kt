package dev.denlogv.lexislearned.data

import java.io.IOException

/**
 * A [SessionStore] that keeps the sessions in memory, for tests of what is stored and when.
 *
 * @param failing makes every write fail like a full disk.
 */
class MemorySessionStore(var failing: Boolean = false) : SessionStore {
    /** The stored sessions by scope. */
    val sessions = HashMap<SessionScope, SavedSession>()

    /** How often a session was written. */
    var writes = 0

    override fun save(session: SavedSession) {
        if (failing) throw IOException("disk full")
        sessions[session.scope] = session
        writes++
    }

    override fun load(scope: SessionScope): SavedSession? = sessions[scope]

    override fun clear(scope: SessionScope) {
        sessions.remove(scope)
    }
}
