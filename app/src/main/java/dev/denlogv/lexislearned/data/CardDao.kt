package dev.denlogv.lexislearned.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Queries on cards: listing, choosing words for a session, saving and resetting progress. */
@Dao
interface CardDao {
    /**
     * Observes the cards of a chapter.
     *
     * @param chapterId the chapter.
     * @return a flow of the cards in reading order.
     */
    @Query("SELECT * FROM cards WHERE chapterId = :chapterId ORDER BY id")
    fun cardsOfChapter(chapterId: Long): Flow<List<CardEntity>>

    /**
     * Loads all cards of a deck.
     *
     * @param deckId the deck.
     * @return the cards in reading order.
     */
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id")
    suspend fun cards(deckId: Long): List<CardEntity>

    /**
     * Words that are due for review and not yet completed.
     *
     * @param deckId the deck.
     * @param chapterId restrict to this chapter, or a negative number for all.
     * @param partId restrict to this part, or a negative number for all.
     * @param needed the number of sessions that completes a word; completed words are left out.
     * @param now the current time in epoch milliseconds.
     * @param limit the most words to return.
     * @return the due words, the longest overdue first.
     */
    @Query(
        """SELECT * FROM cards WHERE deckId = :deckId AND (:chapterId < 0 OR chapterId = :chapterId)
        AND (:partId < 0 OR chapterId IN (SELECT id FROM chapters WHERE partId = :partId))
        AND sessionsDone < :needed AND dueAt IS NOT NULL AND dueAt <= :now ORDER BY dueAt LIMIT :limit""",
    )
    suspend fun dueCards(deckId: Long, chapterId: Long, partId: Long, needed: Int, now: Long, limit: Int): List<CardEntity>

    /**
     * Words that were never studied.
     *
     * @param deckId the deck.
     * @param chapterId restrict to this chapter, or a negative number for all.
     * @param partId restrict to this part, or a negative number for all.
     * @param limit the most words to return.
     * @return the new words in reading order.
     */
    @Query(
        """SELECT * FROM cards WHERE deckId = :deckId AND (:chapterId < 0 OR chapterId = :chapterId)
        AND (:partId < 0 OR chapterId IN (SELECT id FROM chapters WHERE partId = :partId))
        AND dueAt IS NULL ORDER BY id LIMIT :limit""",
    )
    suspend fun newCards(deckId: Long, chapterId: Long, partId: Long, limit: Int): List<CardEntity>

    /**
     * Random cards of a chapter, for filler tiles and wrong answers.
     *
     * @param chapterId the chapter.
     * @param excludeIds cards that must not be returned.
     * @param limit the most cards to return.
     * @return up to [limit] random cards.
     */
    @Query("SELECT * FROM cards WHERE chapterId = :chapterId AND id NOT IN (:excludeIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomInChapter(chapterId: Long, excludeIds: List<Long>, limit: Int): List<CardEntity>

    /**
     * Random cards of a deck, used when a chapter has too few.
     *
     * @param deckId the deck.
     * @param excludeIds cards that must not be returned.
     * @param limit the most cards to return.
     * @return up to [limit] random cards.
     */
    @Query("SELECT * FROM cards WHERE deckId = :deckId AND id NOT IN (:excludeIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomCardsExcluding(deckId: Long, excludeIds: List<Long>, limit: Int): List<CardEntity>

    /**
     * Random words of a chapter that were studied before, for the filler tiles of the Pair board.
     *
     * @param chapterId the chapter.
     * @param excludeIds cards that must not be returned.
     * @param limit the most cards to return.
     * @return up to [limit] random studied cards.
     */
    @Query(
        "SELECT * FROM cards WHERE chapterId = :chapterId AND id NOT IN (:excludeIds) " +
            "AND (dueAt IS NOT NULL OR sessionsDone > 0) ORDER BY RANDOM() LIMIT :limit",
    )
    suspend fun randomLearnedInChapter(chapterId: Long, excludeIds: List<Long>, limit: Int): List<CardEntity>

    /**
     * Random words of a deck that were studied before, used when the chapter has too few.
     *
     * @param deckId the deck.
     * @param excludeIds cards that must not be returned.
     * @param limit the most cards to return.
     * @return up to [limit] random studied cards.
     */
    @Query(
        "SELECT * FROM cards WHERE deckId = :deckId AND id NOT IN (:excludeIds) " +
            "AND (dueAt IS NOT NULL OR sessionsDone > 0) ORDER BY RANDOM() LIMIT :limit",
    )
    suspend fun randomLearnedInDeck(deckId: Long, excludeIds: List<Long>, limit: Int): List<CardEntity>

    /**
     * The study state of the cards of a deck that have any, keyed by source id.
     *
     * @param deckId the deck.
     * @return one row per card that was studied.
     */
    @Query(
        "SELECT sourceId, reps, ease, intervalDays, dueAt, lapses, sessionsDone FROM cards " +
            "WHERE deckId = :deckId AND (dueAt IS NOT NULL OR sessionsDone > 0)",
    )
    suspend fun progressRows(deckId: Long): List<ProgressRow>

    /**
     * Saves a card's study state.
     *
     * @param id the card's database id.
     * @param reps consecutive successful reviews.
     * @param ease SM-2 ease factor.
     * @param intervalDays review interval in days.
     * @param dueAt when the card is due, epoch milliseconds.
     * @param lapses how often the card was failed.
     * @param sessionsDone sessions the word has completed.
     */
    @Query(
        "UPDATE cards SET reps = :reps, ease = :ease, intervalDays = :intervalDays, dueAt = :dueAt, " +
            "lapses = :lapses, sessionsDone = :sessionsDone WHERE id = :id",
    )
    suspend fun updateProgress(id: Long, reps: Int, ease: Double, intervalDays: Int, dueAt: Long?, lapses: Int, sessionsDone: Int)

    /**
     * Inserts cards.
     *
     * @param cards the card rows.
     */
    @Insert
    suspend fun insertCards(cards: List<CardEntity>)

    /**
     * Resets the progress of every card in a deck.
     *
     * @param deckId the deck.
     */
    @Query(SqlFragments.RESET_CARDS + " WHERE deckId = :deckId")
    suspend fun resetDeck(deckId: Long)

    /**
     * Resets the progress of every card in a chapter.
     *
     * @param chapterId the chapter.
     */
    @Query(SqlFragments.RESET_CARDS + " WHERE chapterId = :chapterId")
    suspend fun resetChapter(chapterId: Long)

    /**
     * Resets the progress of every card in a part.
     *
     * @param partId the part.
     */
    @Query(SqlFragments.RESET_CARDS + " WHERE chapterId IN (SELECT id FROM chapters WHERE partId = :partId)")
    suspend fun resetPart(partId: Long)

    /**
     * Resets the progress of one card.
     *
     * @param cardId the card's database id.
     */
    @Query(SqlFragments.RESET_CARDS + " WHERE id = :cardId")
    suspend fun resetCard(cardId: Long)
}
