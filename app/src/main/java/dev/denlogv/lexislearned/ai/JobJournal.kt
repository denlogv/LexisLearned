package dev.denlogv.lexislearned.ai

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Writes the progress of a generation to a [JobStore] as it happens, off the calling thread and in the order of the calls.
 *
 * Saving is best effort: if the disk is full the generation goes on, it just cannot be resumed after a restart. Every call completes
 * even if the caller was cancelled, so that stopping a run never leaves an outdated record behind.
 *
 * @param store where the job is kept.
 */
class JobJournal(private val store: JobStore) {
    // One thread, so a late "forget" can never overtake the "remember" that was called after it.
    private val io = Dispatchers.IO.limitedParallelism(1)

    /**
     * Keeps the book of a job that is about to start.
     *
     * @param epub the contents of the EPUB file.
     */
    suspend fun begin(epub: ByteArray) = guarded { store.saveBook(epub) }

    /**
     * Keeps the progress of the job.
     *
     * @param record the progress.
     */
    suspend fun update(record: JobRecord) = guarded { store.saveRecord(record) }

    /** Forgets the job: it is done, or the user does not want to resume it. */
    suspend fun clear() = guarded { store.clear() }

    /**
     * Reads the job left by an earlier run of the app.
     *
     * @return the job, or null if there is none.
     */
    suspend fun restore(): StoredJob? = withContext(io) { store.load() }

    /**
     * Runs a write on the journal's thread, to completion, and ignores a disk error.
     *
     * @param write the write.
     */
    private suspend fun guarded(write: () -> Unit) = withContext(io + NonCancellable) {
        try {
            write()
        } catch (ignored: IOException) {
            // Best effort, see the class documentation.
        }
    }
}
