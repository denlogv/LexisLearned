package dev.denlogv.lexislearned.ui.deck

import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.ChapterSummary
import dev.denlogv.lexislearned.data.PartEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckLogicTest {
    private fun card(done: Int = 0, due: Long? = null) = CardEntity(
        id = 1, deckId = 1, chapterId = 1, sourceId = "c", frontText = "a", frontTranscription = null, frontExample = null,
        frontGender = null, backText = "b", backExample = null, backGender = null, sessionsDone = done, dueAt = due,
    )

    private fun chapter(id: Long, part: Long?, total: Int = 4, completed: Int = 1, due: Int = 0) = ChapterSummary(
        id, part, "C$id", null, total, completed, 1, completed * 2 + 1, 2, due,
    )

    @Test
    fun statusOfAWordFollowsItsProgress() {
        assertEquals("new", cardStatus(card(), 100, 3).label)
        assertEquals("learning", cardStatus(card(due = 500), 100, 3).label)
        assertEquals("1/3 sessions", cardStatus(card(1, 500), 100, 3).label)
        assertEquals("completed", cardStatus(card(3), 100, 3).label)
        val due = cardStatus(card(1, 50), 100, 3)
        assertTrue(due.due)
        assertEquals("due · 1/3", due.label)
        assertFalse(cardStatus(card(3, 50), 100, 3).due)
    }

    @Test
    fun singularSessionWordIsUsedWhenOneSessionIsNeeded() {
        assertEquals("completed", cardStatus(card(1), 0, 1).label)
        assertEquals("1/2 sessions", cardStatus(card(1, 500), 100, 2).label)
    }

    @Test
    fun hasProgressIsTrueForStudiedWords() {
        assertFalse(hasProgress(card()))
        assertTrue(hasProgress(card(1)))
        assertTrue(hasProgress(card(due = 5)))
    }

    @Test
    fun totalsAddUpChapters() {
        val totals = listOf(chapter(1, null, due = 1), chapter(2, null, due = 2)).totals()
        assertEquals(8, totals.total)
        assertEquals(2, totals.completed)
        assertEquals(3, totals.due)
        assertEquals(2, totals.needed)
        assertEquals(6f / 16f, totals.progress, 0.0001f)
        assertEquals(0f, emptyList<ChapterSummary>().totals().progress, 0f)
        assertEquals(chapter(1, null).toTotals().total, 4)
    }

    @Test
    fun entriesGroupPartChaptersAtTheFirstChapter() {
        val parts = listOf(PartEntity(10, 1, "p", "Part", null))
        val entries = buildEntries(parts, listOf(chapter(1, null), chapter(2, 10), chapter(3, 10), chapter(4, null)))
        assertEquals(listOf("c1", "p10", "c4"), entries.map { it.key })
        val part = entries[1] as DeckEntry.PartRow
        assertEquals(2, part.chapters)
        assertEquals(8, part.totals.total)
    }

    @Test
    fun chapterWithUnknownPartStaysLoose() {
        val entries = buildEntries(emptyList(), listOf(chapter(1, 99)))
        assertTrue(entries.single() is DeckEntry.ChapterRow)
    }
}
