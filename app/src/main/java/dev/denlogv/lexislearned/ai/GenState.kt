package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.epub.EpubBook

/** The stages of creating a deck from an EPUB, as shown on the EPUB screen. */
sealed interface GenState {
    /** Nothing has been chosen yet. */
    data object Idle : GenState

    /** The EPUB is being read. */
    data object Loading : GenState

    /**
     * The book was read and awaits review.
     *
     * @property book the book with its detected sections.
     */
    data class Ready(val book: EpubBook) : GenState

    /**
     * Tells [GenerationManager.start] to add to a deck that was paused instead of creating a new one.
     *
     * @property deckId the stored deck to add to, or null if the paused generation had not stored a chapter yet, in which case a
     * new deck is created.
     * @property selection indexes of the sections to preselect: those that were chosen and are not in the deck yet.
     */
    data class Continuation(val deckId: Long?, val selection: Set<Int>)

    /**
     * Cards are being generated.
     *
     * @property done sections finished.
     * @property total sections to process.
     * @property cards cards collected so far.
     * @property message what is being worked on.
     * @property title the book's title.
     * @property deckId the deck in the library once its first chapter is stored, null before that; later chapters are added
     * to it as they are finished.
     * @property pausing whether a pause was asked for and the current section is being finished first.
     */
    data class Running(
        val done: Int,
        val total: Int,
        val cards: Int,
        val message: String,
        val title: String = "",
        val deckId: Long? = null,
        val pausing: Boolean = false,
    ) : GenState

    /**
     * Generation stopped before every chosen section is in the deck, and can be resumed: the user paused it, a section failed, or the
     * app was closed or killed while it ran (the state is then restored on the next launch).
     *
     * @property deckId the deck in the library with the sections that are done, null if none was stored yet.
     * @property cards number of cards in it.
     * @property book the book, so the rest can be done without choosing the file again.
     * @property remaining indexes of the sections that are not in the deck: failed, interrupted or not reached.
     * @property failed descriptions of the sections that failed, or the reason generation could not go on; empty for a plain pause.
     */
    data class Paused(
        val deckId: Long?,
        val cards: Int,
        val book: EpubBook,
        val remaining: Set<Int>,
        val failed: List<String> = emptyList(),
    ) : GenState

    /**
     * Every chosen section is in the deck.
     *
     * @property deckId the id of the new deck in the library.
     * @property cards number of cards in it.
     */
    data class Finished(val deckId: Long, val cards: Int) : GenState

    /**
     * Something went wrong.
     *
     * @property message what happened, for the user.
     * @property book the book that was being processed, if any, so the user can go back to its review without choosing it again.
     */
    data class Failed(val message: String, val book: EpubBook? = null) : GenState
}
