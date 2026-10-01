package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.epub.EpubReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubTest {
    private fun fakeEpub(): ByteArray = loremEpub()

    @Test
    fun readsChaptersAndSkipsFrontMatter() {
        val book = EpubReader.read(fakeEpub().inputStream())
        assertEquals("Lorem Book", book.title)
        assertEquals("en-GB", book.language)
        assertEquals(listOf("Copyright", "Chapter One", "Chapter Two"), book.chapters.map { it.title })
        assertEquals(setOf(2, 3), book.defaultSelection) // copyright page is flagged, not dropped
        assertTrue(book.chapters[2].text.contains("& more text"))
    }

    @Test
    fun readsBundledSampleEpub() {
        val file = java.io.File("../samples/lighthouse-sample.epub")
        val book = file.inputStream().use { EpubReader.read(it) }
        assertEquals("The Lighthouse Niece", book.title)
        assertEquals(listOf("The Arrival", "The Keeper", "The Storm"), book.chapters.map { it.title })
    }
}
