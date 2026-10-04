package dev.denlogv.lexislearned.data

import kotlinx.coroutines.flow.Flow

/**
 * The stored decks as the user sees them: observing decks, parts, chapters and cards with their progress, and managing them
 * by deleting a deck or resetting progress.
 *
 * @param db the database.
 * @param clock the current time in epoch milliseconds, which decides what counts as due; tests replace it.
 */
class DeckLibrary(db: AppDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val structure = db.structureDao()
    private val summaries = db.summaryDao()
    private val cards = db.cardDao()

    /**
     * Observes all decks with their progress.
     *
     * @param needed the number of sessions that completes a word.
     * @return a flow of the decks ordered by title.
     */
    fun decks(needed: Int): Flow<List<DeckSummary>> = summaries.deckSummaries(clock(), needed)

    /**
     * Observes the chapters of a deck with their progress.
     *
     * @param deckId the deck.
     * @param needed the number of sessions that completes a word.
     * @return a flow of the chapters in reading order.
     */
    fun chapters(deckId: Long, needed: Int): Flow<List<ChapterSummary>> = summaries.chapterSummaries(deckId, clock(), needed)

    /**
     * Observes one chapter's summary.
     *
     * @param id the chapter.
     * @param needed the number of sessions that completes a word.
     * @return a flow of the chapter with its progress, or null if it does not exist.
     */
    fun chapterSummary(id: Long, needed: Int): Flow<ChapterSummary?> = summaries.chapterSummary(id, clock(), needed)

    /**
     * Observes one chapter.
     *
     * @param id the chapter.
     * @return a flow of the chapter, or null if it does not exist.
     */
    fun chapter(id: Long): Flow<ChapterEntity?> = structure.chapterFlow(id)

    /**
     * Observes the cards of a chapter.
     *
     * @param id the chapter.
     * @return a flow of the cards in reading order.
     */
    fun cardsOfChapter(id: Long): Flow<List<CardEntity>> = cards.cardsOfChapter(id)

    /**
     * Observes the parts of a deck.
     *
     * @param deckId the deck.
     * @return a flow of the parts in order.
     */
    fun parts(deckId: Long): Flow<List<PartEntity>> = structure.parts(deckId)

    /**
     * Observes one part.
     *
     * @param id the part.
     * @return a flow of the part, or null if it does not exist.
     */
    fun part(id: Long): Flow<PartEntity?> = structure.partFlow(id)

    /**
     * Observes one deck.
     *
     * @param deckId the deck.
     * @return a flow of the deck, or null if it does not exist.
     */
    fun deck(deckId: Long): Flow<DeckEntity?> = structure.deckFlow(deckId)

    /**
     * Deletes a deck with everything in it.
     *
     * @param deckId the deck.
     */
    suspend fun delete(deckId: Long) = structure.deleteDeck(deckId)

    /**
     * Makes a word new again.
     *
     * @param id the card.
     */
    suspend fun resetCard(id: Long) = cards.resetCard(id)

    /**
     * Makes every word of a chapter new again.
     *
     * @param id the chapter.
     */
    suspend fun resetChapter(id: Long) = cards.resetChapter(id)

    /**
     * Makes every word of a part new again.
     *
     * @param id the part.
     */
    suspend fun resetPart(id: Long) = cards.resetPart(id)

    /**
     * Makes every word of a deck new again.
     *
     * @param deckId the deck.
     */
    suspend fun resetProgress(deckId: Long) = cards.resetDeck(deckId)
}
