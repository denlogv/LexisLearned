package dev.denlogv.lexislearned.data

import java.io.File
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A [SessionStore] that keeps each session as a JSON file of a private directory, one file per scope.
 *
 * @param dir the directory; it is created when something is stored.
 */
class FileSessionStore(private val dir: File) : SessionStore {
    /**
     * Writes the file of the session's scope.
     *
     * @param session the session.
     * @throws IOException if it cannot be written.
     */
    override fun save(session: SavedSession) =
        writeAtomically(file(session.scope), JSON.encodeToString(SavedSession.serializer(), session).toByteArray())

    /**
     * Reads the file of a scope.
     *
     * @param scope what the session covers.
     * @return the session, or null if the file is missing or damaged.
     */
    override fun load(scope: SessionScope): SavedSession? = try {
        JSON.decodeFromString(SavedSession.serializer(), file(scope).readText())
    } catch (ignored: IOException) {
        null // Missing or unreadable: there is nothing to continue.
    } catch (ignored: SerializationException) {
        null // Written by a version that stored something else, or damaged.
    }

    /**
     * Deletes the file of a scope.
     *
     * @param scope what the session covers.
     */
    override fun clear(scope: SessionScope) {
        file(scope).delete()
    }

    /**
     * The file of a scope.
     *
     * @param scope what the session covers.
     * @return where its session is kept.
     */
    private fun file(scope: SessionScope): File = File(dir, "session-${scope.key}.json")

    private companion object {
        /** Reads sessions that have fields this version no longer writes. */
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
