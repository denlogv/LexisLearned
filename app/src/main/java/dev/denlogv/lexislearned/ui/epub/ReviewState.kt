package dev.denlogv.lexislearned.ui.epub

import androidx.compose.ui.state.ToggleableState
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.epub.EpubBook
import dev.denlogv.lexislearned.epub.EpubChapter
import dev.denlogv.lexislearned.ui.plural
import kotlin.math.roundToInt

/** Cards requested per 1,000 words unless the user changes it. */
const val DEFAULT_DENSITY = 8

/** Fewest cards asked for from one section. */
private const val MIN_CARDS = 3

/** Most cards asked for from one section. */
private const val MAX_CARDS = 60

/** Rough number of model input tokens (in thousands) per word of book text, including the instructions around it. */
private const val TOKENS_PER_WORD = 1.4

/** Books at least this long are summarised in thousands of words. */
private const val THOUSAND = 1000

/**
 * The choices made while reviewing a book before generating a deck.
 *
 * @property selected indexes of the sections that will be turned into cards.
 * @property lang the book's language code.
 * @property density cards requested per 1,000 words.
 */
data class ReviewState(val selected: Set<Int>, val lang: String, val density: Int = DEFAULT_DENSITY) {
    /**
     * Selects or deselects a single section.
     *
     * @param index the section's index.
     * @return the new state.
     */
    fun toggleChapter(index: Int): ReviewState = copy(selected = if (index in selected) selected - index else selected + index)

    /**
     * Selects all sections of a part, or deselects them if they are all selected already.
     *
     * @param group the part's sections.
     * @return the new state.
     */
    fun togglePart(group: ChapterGroup): ReviewState {
        val ids = group.chapters.map { it.index }.toSet()
        return copy(selected = if (ids.all { it in selected }) selected - ids else selected + ids)
    }

    /**
     * Estimates the size of the request.
     *
     * @param book the book.
     * @return the expected number of cards and input tokens for the current selection.
     */
    fun estimate(book: EpubBook): Estimate {
        val chosen = book.chapters.filter { it.index in selected }
        return Estimate(chosen.sumOf { cardsFor(it, density) }, (chosen.sumOf { it.words } * TOKENS_PER_WORD / THOUSAND).roundToInt())
    }

    /** Creates the starting choices. */
    companion object {
        /**
         * The starting choices for a book: its suggested sections and its declared language (English if none). When the rest of
         * a book is added to a deck, only the sections that are not in the deck yet are chosen.
         *
         * @param book the book.
         * @param continuation the deck that is continued, or null for a new deck.
         * @return the initial state.
         */
        fun initial(book: EpubBook, continuation: GenState.Continuation? = null): ReviewState =
            ReviewState(continuation?.selection ?: book.defaultSelection, book.language.take(2).lowercase().ifBlank { "en" })
    }
}

/**
 * The expected size of a generation request.
 *
 * @property cards number of cards.
 * @property tokensK input tokens in thousands.
 */
data class Estimate(val cards: Int, val tokensK: Int)

/**
 * Consecutive sections of one part, or a single section that belongs to no part.
 *
 * @property part the part's title, or null for a loose section.
 * @property chapters the sections in reading order.
 */
data class ChapterGroup(val part: String?, val chapters: List<EpubChapter>)

/**
 * Groups sections by part: neighbours of the same part form one group, sections without a part stand alone.
 *
 * @param chapters the sections in reading order.
 * @return the groups in reading order.
 */
fun groupByPart(chapters: List<EpubChapter>): List<ChapterGroup> {
    val out = ArrayList<ChapterGroup>()
    for (c in chapters) {
        val last = out.lastOrNull()
        if (c.part != null && last?.part == c.part) {
            out[out.lastIndex] = last.copy(chapters = last.chapters + c)
        } else {
            out += ChapterGroup(c.part, listOf(c))
        }
    }
    return out
}

/**
 * How many cards to ask for from one section.
 *
 * @param chapter the section.
 * @param density cards per 1,000 words.
 * @return the count, kept between 3 and 60.
 */
fun cardsFor(chapter: EpubChapter, density: Int): Int = (chapter.words * density / THOUSAND).coerceIn(MIN_CARDS, MAX_CARDS)

/**
 * The state of a part's checkbox.
 *
 * @param group the part's sections.
 * @param selected indexes of the selected sections.
 * @return on if all are selected, off if none, otherwise indeterminate.
 */
fun partToggleState(group: ChapterGroup, selected: Set<Int>): ToggleableState = when (group.chapters.count { it.index in selected }) {
    0 -> ToggleableState.Off
    group.chapters.size -> ToggleableState.On
    else -> ToggleableState.Indeterminate
}

/**
 * The line under the book title: sections, parts and length.
 *
 * @param book the book.
 * @return for example "12 sections in 3 parts · ~45k words".
 */
fun bookSummary(book: EpubBook): String {
    val words = book.chapters.sumOf { it.words }
    val length = if (words < THOUSAND) plural(words, "word") else "~${words / THOUSAND}k words"
    val parts = if (book.parts.isNotEmpty()) " in ${plural(book.parts.size, "part")}" else ""
    return plural(book.chapters.size, "section") + parts + " · " + length
}

/**
 * The line under a section's title: its length, expected cards and why it is skipped by default.
 *
 * @param chapter the section.
 * @param density cards per 1,000 words.
 * @return for example "1,200 words · ~9 cards · front/back matter".
 */
fun chapterDetail(chapter: EpubChapter, density: Int): String =
    "${plural(chapter.words, "word")} · ~${plural(cardsFor(chapter, density), "card")}" + (chapter.skipReason?.let { " · $it" } ?: "")
