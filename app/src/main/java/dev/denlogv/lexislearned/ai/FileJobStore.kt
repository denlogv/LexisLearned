package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.data.writeAtomically
import java.io.File
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A [JobStore] that keeps the job in two files of a private directory: the book as it was picked, and the record as JSON.
 *
 * @param dir the directory; it is created when something is stored.
 */
class FileJobStore(private val dir: File) : JobStore {
    private val book get() = File(dir, BOOK_FILE)
    private val record get() = File(dir, RECORD_FILE)

    /**
     * Writes the book file.
     *
     * @param epub the contents of the EPUB file.
     * @throws IOException if it cannot be written.
     */
    override fun saveBook(epub: ByteArray) = writeAtomically(book, epub)

    /**
     * Writes the record file.
     *
     * @param record the progress.
     * @throws IOException if it cannot be written.
     */
    override fun saveRecord(record: JobRecord) =
        writeAtomically(this.record, JSON.encodeToString(JobRecord.serializer(), record).toByteArray())

    /**
     * Reads both files.
     *
     * @return the job, or null if a file is missing or the record is damaged.
     */
    override fun load(): StoredJob? = try {
        StoredJob(JSON.decodeFromString(JobRecord.serializer(), record.readText()), book.readBytes())
    } catch (ignored: IOException) {
        null // Missing or unreadable: there is nothing to resume.
    } catch (ignored: SerializationException) {
        null // A record from a version that wrote something else, or a damaged file.
    }

    /** Deletes both files. */
    override fun clear() {
        record.delete()
        book.delete()
    }

    private companion object {
        const val BOOK_FILE = "book.epub"
        const val RECORD_FILE = "job.json"

        /** Reads records that have fields this version no longer writes, so a job paused before an update can still be resumed. */
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
