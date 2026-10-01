package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.epub.EpubReader
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Optional: EPUB_SAMPLE=/path/book.epub prints how the reader splits a real book. */
class EpubSmokeTest {
    @Test
    fun printsChapterSplit() {
        val path = System.getenv("EPUB_SAMPLE")
        assumeTrue(path != null && File(path).exists())
        val book = File(path!!).inputStream().use { EpubReader.read(it) }
        println("BOOK ${book.title} lang=${book.language} chapters=${book.chapters.size} parts=${book.parts}")
        var lastPart: String? = "\u0000"
        book.chapters.forEach {
            if (it.part != lastPart) {
                println("PART ${it.part}")
                lastPart = it.part
            }
            println("CH ${it.index} | ${it.title} | words=${it.words}${it.skipReason?.let { r -> " | SKIP: $r" } ?: ""}")
        }
    }
}
