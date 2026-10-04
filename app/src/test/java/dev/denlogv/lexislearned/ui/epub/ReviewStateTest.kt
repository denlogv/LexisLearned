package dev.denlogv.lexislearned.ui.epub

import androidx.compose.ui.state.ToggleableState
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import dev.denlogv.lexislearned.epub.EpubChapter
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewStateTest {
    private fun words(n: Int) = List(n) { "w$it" }.joinToString(" ")

    private val book = EpubBook(
        "Lorem",
        "de-AT",
        listOf(
            EpubChapter(1, "Contents", words(10), null, "front/back matter"),
            EpubChapter(2, "One", words(2000), "Part I"),
            EpubChapter(3, "Two", words(1000), "Part I"),
            EpubChapter(4, "Three", words(10)),
        ),
    )

    @Test
    fun continuingADeckStartsWithOnlyTheMissingSections() {
        val s = ReviewState.initial(book, GenState.Continuation(deckId = 5, selection = setOf(3)))
        assertEquals(setOf(3), s.selected)
        assertEquals("de", s.lang)
    }

    @Test
    fun initialStateUsesSuggestionsAndDeclaredLanguage() {
        val s = ReviewState.initial(book)
        assertEquals(setOf(2, 3, 4), s.selected)
        assertEquals("de", s.lang)
        assertEquals("en", ReviewState.initial(book.copy(language = "")).lang)
    }

    @Test
    fun togglingChaptersAndParts() {
        val group = groupByPart(book.chapters)[1]
        var s = ReviewState(setOf(2), "en")
        s = s.toggleChapter(3)
        assertEquals(setOf(2, 3), s.selected)
        assertEquals(setOf(3), s.toggleChapter(2).selected)
        assertEquals(setOf<Int>(), s.togglePart(group).selected)
        assertEquals(setOf(2, 3), ReviewState(setOf(), "en").togglePart(group).selected)
    }

    @Test
    fun groupsKeepNeighboursOfAPartTogether() {
        val groups = groupByPart(book.chapters)
        assertEquals(listOf(null, "Part I", null), groups.map { it.part })
        assertEquals(2, groups[1].chapters.size)
    }

    @Test
    fun partCheckboxHasThreeStates() {
        val group = groupByPart(book.chapters)[1]
        assertEquals(ToggleableState.Off, partToggleState(group, emptySet()))
        assertEquals(ToggleableState.Indeterminate, partToggleState(group, setOf(2)))
        assertEquals(ToggleableState.On, partToggleState(group, setOf(2, 3)))
    }

    @Test
    fun cardCountIsClampedAndEstimated() {
        assertEquals(3, cardsFor(book.chapters[3], 8))
        assertEquals(16, cardsFor(book.chapters[1], 8))
        assertEquals(60, cardsFor(EpubChapter(9, "x", words(100000)), 20))
        val est = ReviewState(setOf(2, 3), "en").estimate(book)
        assertEquals(24, est.cards)
        assertEquals(4, est.tokensK)
    }

    @Test
    fun summariesDescribeBookAndSections() {
        assertEquals("4 sections in 1 part · ~3k words", bookSummary(book))
        assertEquals("1 section · 10 words", bookSummary(EpubBook("x", "en", listOf(book.chapters[3]))))
        assertEquals("10 words · ~3 cards · front/back matter", chapterDetail(book.chapters[0], 8))
        assertEquals("10 words · ~3 cards", chapterDetail(book.chapters[3], 8))
    }
}
