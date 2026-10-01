package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.domain.CardProgress
import dev.denlogv.lexislearned.domain.PartRef
import dev.denlogv.lexislearned.format.FormatException
import dev.denlogv.lexislearned.format.FormatRegistry
import dev.denlogv.lexislearned.format.NativeFormat
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FormatTest {
    @Test
    fun nativeFormatRoundTripsIncludingProgress() {
        val deck = sampleDeck().let { d ->
            d.copy(
                chapters = d.chapters.map { ch ->
                    ch.copy(cards = ch.cards.map { it.copy(progress = CardProgress(reps = 2, dueAt = 5L)) })
                },
            )
        }
        val out = ByteArrayOutputStream().also { NativeFormat.write(deck, it) }
        assertEquals(deck, NativeFormat.read(out.toByteArray().inputStream()))
    }

    @Test
    fun registrySniffsFormats() {
        val native = ByteArrayOutputStream().also { NativeFormat.write(sampleDeck(), it) }.toByteArray()
        assertEquals(sampleDeck().cardCount, FormatRegistry.read("x.bin", native).cardCount)
        assertNotNull(FormatRegistry.byExtension("Deck.LEXIS"))
        assertThrows(FormatException::class.java) { FormatRegistry.read("deck.xyz", byteArrayOf(8, 1)) }
    }

    private fun partedDeck() = sampleDeck().let { d ->
        val p1 = PartRef("p1", "Book One", "Книга первая")
        d.copy(chapters = d.chapters.mapIndexed { i, ch -> if (i == 0) ch else ch.copy(part = p1) })
    }

    @Test
    fun nativeFormatKeepsParts() {
        val out = ByteArrayOutputStream().also { NativeFormat.write(partedDeck(), it) }
        assertEquals(partedDeck(), NativeFormat.read(out.toByteArray().inputStream()))
    }
}
