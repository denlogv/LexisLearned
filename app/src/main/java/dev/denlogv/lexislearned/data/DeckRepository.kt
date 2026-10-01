package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.Scheduler
import dev.denlogv.lexislearned.domain.SessionGrading
import dev.denlogv.lexislearned.domain.Sm2Scheduler
import kotlinx.coroutines.flow.Flow

/**
 * The app's window onto stored decks: browsing, choosing words for a session, saving progress, resetting, importing and
 * exporting.
 *
 * @param db the database.
 * @param scheduler decides when a word is due after a session.
 * @param clock the current time in epoch milliseconds; tests replace it.
 */
class DeckRepository(
    db: AppDatabase,
    private val scheduler: Scheduler = Sm2Scheduler,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val structure = db.structureDao()
    private val summaries = db.summaryDao()
    private val cards = db.cardDao()
    private val importer = DeckImporter(db)
    private val exporter = DeckExporter(db)

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
     * Imports a deck; re-importing the same source deck replaces it but keeps study progress.
     *
     * @param deck the deck to store.
     * @return the database id of the stored deck.
     */
    suspend fun import(deck: Deck): Long = importer.import(deck)

    /**
     * Loads a deck as a domain object, including progress.
     *
     * @param deckId the deck.
     * @return the deck.
     * @throws IllegalStateException if there is no such deck.
     */
    suspend fun export(deckId: Long): Deck = exporter.export(deckId)

    /**
     * Chooses the words for a session: due words first, then new ones.
     *
     * @param deckId the deck.
     * @param chapterId restrict to this chapter, or null for all.
     * @param partId restrict to this part, or null for all.
     * @param needed the number of sessions that completes a word; completed words are not offered.
     * @param size the most words in the session.
     * @param newLimit the most never-studied words to add.
     * @return the session's words.
     */
    suspend fun sessionCards(deckId: Long, chapterId: Long?, partId: Long?, needed: Int, size: Int, newLimit: Int): List<CardEntity> {
        val chapter = chapterId ?: NO_FILTER
        val part = partId ?: NO_FILTER
        val due = cards.dueCards(deckId, chapter, part, needed, clock(), size)
        val fresh = cards.newCards(deckId, chapter, part, minOf(newLimit, size - due.size).coerceAtLeast(0))
        return due + fresh
    }

    /**
     * Wrong answers for Select: other words from the same chapter first, the rest of the book only if needed.
     *
     * @param card the word being asked.
     * @param count how many wrong answers are wanted.
     * @return up to [count] other words.
     */
    suspend fun distractors(card: CardEntity, count: Int): List<CardEntity> = decoys(card, listOf(card.id), count)

    /**
     * Filler words for the Pair board: random words from the same chapter as [card], topped up from the rest of the book
     * only when the chapter has too few.
     *
     * @param card a word whose chapter to draw from.
     * @param excludeIds words that must not be returned.
     * @param count how many words are wanted.
     * @return up to [count] words.
     */
    suspend fun decoys(card: CardEntity, excludeIds: List<Long>, count: Int): List<CardEntity> {
        val exclude = excludeIds.ifEmpty { listOf(NO_CARD) }
        val same = cards.randomInChapter(card.chapterId, exclude, count)
        if (same.size >= count) return same
        return same + cards.randomCardsExcluding(card.deckId, exclude + same.map { it.id }, count - same.size)
    }

    /**
     * Saves the result of one finished session for a word. The session counts toward completion unless it went badly.
     *
     * @param card the word before the session.
     * @param grade the session's grade, from the first-try accuracy.
     * @param spaced if true spaced-repetition intervals apply, otherwise the word can be studied again straight away.
     * @return the word with its new state.
     */
    suspend fun finishSession(card: CardEntity, grade: Grade, spaced: Boolean): CardEntity {
        val now = clock()
        val next = scheduler.review(card.progress, grade, now)
        val counted = SessionGrading.counts(grade)
        val sessions = card.sessionsDone + if (counted) 1 else 0
        val dueAt = if (!spaced && counted) now else next.dueAt
        cards.updateProgress(card.id, next.reps, next.ease, next.intervalDays, dueAt, next.lapses, sessions)
        return card.copy(
            reps = next.reps,
            ease = next.ease,
            intervalDays = next.intervalDays,
            dueAt = dueAt,
            lapses = next.lapses,
            sessionsDone = sessions,
        )
    }

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

    /**
     * Deletes a deck with everything in it.
     *
     * @param deckId the deck.
     */
    suspend fun delete(deckId: Long) = structure.deleteDeck(deckId)

    private companion object {
        /** Passed to queries to mean "do not filter". */
        const val NO_FILTER = -1L

        /** An id no card has, so an exclusion list is never empty. */
        const val NO_CARD = -1L
    }
}
