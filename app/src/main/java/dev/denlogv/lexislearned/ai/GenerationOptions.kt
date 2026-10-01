package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.Deck

/**
 * How a deck is generated from a book.
 *
 * @property selected indexes of the sections to process; null means the book's default selection.
 * @property sourceLang the book's language code; blank falls back to the book's own declaration, then English.
 * @property cardsPer1000Words how many cards to ask for per thousand words of a section.
 * @property minCards fewest cards asked for from one section.
 * @property maxCards most cards asked for from one section.
 * @property onProgress called before each section with (sections done, total, cards so far, title of the section).
 * @property isCancelled polled between sections; when it returns true generation stops and keeps what is done.
 */
data class GenerationOptions(
    val selected: Set<Int>? = null,
    val sourceLang: String = "",
    val cardsPer1000Words: Int = 8,
    val minCards: Int = 3,
    val maxCards: Int = 60,
    val onProgress: (done: Int, total: Int, cardsSoFar: Int, message: String) -> Unit = { _, _, _, _ -> },
    val isCancelled: () -> Boolean = { false },
)

/**
 * The outcome of generating a deck.
 *
 * @property deck the deck made from the sections that worked.
 * @property failedChapters descriptions of the sections that failed and were skipped.
 */
class GenerationResult(val deck: Deck, val failedChapters: List<String>)
