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
     * Cards are being generated.
     *
     * @property done sections finished.
     * @property total sections to process.
     * @property cards cards collected so far.
     * @property message what is being worked on.
     */
    data class Running(val done: Int, val total: Int, val cards: Int, val message: String) : GenState

    /**
     * The deck was created and imported.
     *
     * @property deckId the id of the new deck in the library.
     * @property cards number of cards in it.
     * @property failed descriptions of sections that failed and were skipped.
     */
    data class Finished(val deckId: Long, val cards: Int, val failed: List<String>) : GenState

    /**
     * Something went wrong.
     *
     * @property message what happened, for the user.
     * @property book the book that was being processed, if any, so the user can retry.
     */
    data class Failed(val message: String, val book: EpubBook? = null) : GenState
}
