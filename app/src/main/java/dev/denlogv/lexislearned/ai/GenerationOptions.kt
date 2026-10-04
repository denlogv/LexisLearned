package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck

/**
 * How a deck is generated from a book.
 *
 * @property selected indexes of the sections to process; null means the book's default selection.
 * @property sourceLang the book's language code; blank falls back to the book's own declaration, then English.
 * @property cardsPer1000Words how many cards to ask for per thousand words of a section.
 * @property minCards fewest cards asked for from one section.
 * @property maxCards most cards asked for from one section.
 * @property onProgress called before each section, and at the end, with (sections whose chapter is in the deck so far, total sections,
 * cards so far, title of the section being worked on). A section that failed or had nothing new is not counted.
 * @property isCancelled polled between sections; when it returns true generation stops and keeps what is done.
 * @property onChapter called with each chapter as soon as its cards are ready, together with the deck's details (title,
 * languages, id) and no chapters, so a caller can store the deck while later sections are still being processed.
 */
data class GenerationOptions(
    val selected: Set<Int>? = null,
    val sourceLang: String = "",
    val cardsPer1000Words: Int = 8,
    val minCards: Int = 3,
    val maxCards: Int = 60,
    val onProgress: (done: Int, total: Int, cardsSoFar: Int, message: String) -> Unit = { _, _, _, _ -> },
    val isCancelled: () -> Boolean = { false },
    val onChapter: suspend (deck: Deck, chapter: Chapter) -> Unit = { _, _ -> },
)

/**
 * The outcome of generating a deck.
 *
 * @property deck the deck made from the sections that worked.
 * @property failedChapters descriptions of the sections that failed and were skipped.
 */
class GenerationResult(val deck: Deck, val failedChapters: List<String>)
