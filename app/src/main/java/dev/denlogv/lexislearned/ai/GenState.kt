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
     * Tells [GenerationManager.start] to add to a deck that was stopped early instead of creating a new one.
     *
     * @property deckId the stored deck to add to.
     * @property selection indexes of the sections to preselect: those that were chosen and are not in the deck yet.
     */
    data class Continuation(val deckId: Long, val selection: Set<Int>)

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
     */
    data class Running(
        val done: Int,
        val total: Int,
        val cards: Int,
        val message: String,
        val title: String = "",
        val deckId: Long? = null,
    ) : GenState

    /**
     * The deck was created and imported.
     *
     * @property deckId the id of the new deck in the library.
     * @property cards number of cards in it.
     * @property failed descriptions of sections that failed and were skipped.
     * @property book the book, kept so the rest can be added later; null if there is nothing to add.
     * @property unfinished indexes of the sections that were chosen but are not in the deck: failed, interrupted or not reached.
     * If there are any, the user can continue with them, and the new chapters are added to this deck.
     */
    data class Finished(
        val deckId: Long,
        val cards: Int,
        val failed: List<String>,
        val book: EpubBook? = null,
        val unfinished: Set<Int> = emptySet(),
    ) : GenState

    /**
     * Something went wrong.
     *
     * @property message what happened, for the user.
     * @property book the book that was being processed, if any, so the user can retry.
     */
    data class Failed(val message: String, val book: EpubBook? = null) : GenState
}
