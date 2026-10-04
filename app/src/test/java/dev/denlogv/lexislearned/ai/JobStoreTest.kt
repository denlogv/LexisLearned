package dev.denlogv.lexislearned.ai

import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JobStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private val record = JobRecord(7, 12, setOf(3, 5), listOf("Chapter (busy)"), "en", 8)
    private val dir get() = File(folder.root, "generation")

    @Test
    fun aStoredJobComesBackAsItWas() {
        val store = FileJobStore(dir)
        store.saveBook(byteArrayOf(1, 2, 3))
        store.saveRecord(record)
        val job = FileJobStore(dir).load()!! // a new object, as after a restart
        assertEquals(record, job.record)
        assertArrayEquals(byteArrayOf(1, 2, 3), job.epub)
    }

    @Test
    fun theLatestRecordWinsAndLeavesNoTemporaryFiles() {
        val store = FileJobStore(dir)
        store.saveBook(byteArrayOf(1))
        store.saveRecord(record)
        store.saveRecord(record.copy(remaining = setOf(5)))
        assertEquals(setOf(5), store.load()!!.record.remaining)
        assertEquals(listOf("book.epub", "job.json"), dir.list()!!.sorted())
    }

    @Test
    fun nothingIsFoundWhenNothingWasStoredOrOnlyHalfOfIt() {
        val store = FileJobStore(dir)
        assertNull(store.load())
        store.saveRecord(record)
        assertNull(store.load()) // no book
    }

    @Test
    fun aDamagedRecordIsNotAJob() {
        val store = FileJobStore(dir)
        store.saveBook(byteArrayOf(1))
        File(dir, "job.json").writeText("{not json")
        assertNull(store.load())
    }

    @Test
    fun clearingForgetsTheJob() {
        val store = FileJobStore(dir)
        store.saveBook(byteArrayOf(1))
        store.saveRecord(record)
        store.clear()
        assertNull(store.load())
        assertFalse(File(dir, "book.epub").exists())
        store.clear() // nothing left: still fine
    }

    @Test
    fun theJournalPassesWritesThrough() = runBlocking {
        val store = MemoryJobStore()
        val journal = JobJournal(store)
        journal.begin(byteArrayOf(9))
        journal.update(record)
        assertEquals(record, journal.restore()!!.record)
        journal.clear()
        assertNull(journal.restore())
    }

    @Test
    fun aFullDiskDoesNotStopTheJournal() = runBlocking {
        val store = MemoryJobStore(failing = true)
        val journal = JobJournal(store)
        journal.begin(byteArrayOf(9))
        journal.update(record)
        assertNull(journal.restore())
        assertEquals(0, store.recordWrites)
    }
}
