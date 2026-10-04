package dev.denlogv.lexislearned.ai

import java.io.IOException

/**
 * Keeps one unfinished generation so it can be resumed after the app was closed or killed. Calls block; [JobJournal] runs them off
 * the main thread and treats a failure as "not saved" rather than as a reason to stop generating.
 */
interface JobStore {
    /**
     * Stores the book of the job, replacing an earlier one.
     *
     * @param epub the contents of the EPUB file.
     * @throws IOException if it cannot be written.
     */
    fun saveBook(epub: ByteArray)

    /**
     * Stores the progress of the job, replacing an earlier record. It is never half written.
     *
     * @param record the progress.
     * @throws IOException if it cannot be written.
     */
    fun saveRecord(record: JobRecord)

    /**
     * Reads the job.
     *
     * @return the job, or null if there is none or what is stored cannot be read.
     */
    fun load(): StoredJob?

    /** Forgets the job; nothing happens if there is none. */
    fun clear()
}
