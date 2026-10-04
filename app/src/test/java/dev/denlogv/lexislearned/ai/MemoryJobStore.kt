package dev.denlogv.lexislearned.ai

import java.io.IOException

/**
 * A [JobStore] that keeps the job in memory, for tests of what is stored and when.
 *
 * @param failing makes every write fail like a full disk.
 */
class MemoryJobStore(var failing: Boolean = false) : JobStore {
    /** The stored book, or null. */
    var epub: ByteArray? = null

    /** The stored record, or null. */
    var record: JobRecord? = null

    /** How often a record was written. */
    var recordWrites = 0

    override fun saveBook(epub: ByteArray) {
        if (failing) throw IOException("disk full")
        this.epub = epub
    }

    override fun saveRecord(record: JobRecord) {
        if (failing) throw IOException("disk full")
        this.record = record
        recordWrites++
    }

    override fun load(): StoredJob? {
        val book = epub
        val saved = record
        return if (book != null && saved != null) StoredJob(saved, book) else null
    }

    override fun clear() {
        epub = null
        record = null
    }
}
