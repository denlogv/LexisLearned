package dev.denlogv.lexislearned.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Queries that summarise progress per deck and per chapter. */
@Dao
interface SummaryDao {
    /**
     * Observes all decks with their progress.
     *
     * @param now the current time in epoch milliseconds, to count due words.
     * @param needed the number of sessions that completes a word.
     * @return a flow of the decks ordered by title.
     */
    @Query(SqlFragments.DECK_SUMMARY + " ORDER BY d.title COLLATE NOCASE")
    fun deckSummaries(now: Long, needed: Int): Flow<List<DeckSummary>>

    /**
     * Observes the chapters of a deck with their progress.
     *
     * @param deckId the deck.
     * @param now the current time in epoch milliseconds.
     * @param needed the number of sessions that completes a word.
     * @return a flow of the chapters in reading order.
     */
    @Query(SqlFragments.CHAPTER_SUMMARY + " WHERE h.deckId = :deckId ORDER BY h.id")
    fun chapterSummaries(deckId: Long, now: Long, needed: Int): Flow<List<ChapterSummary>>

    /**
     * Observes one chapter with its progress.
     *
     * @param id the chapter's database id.
     * @param now the current time in epoch milliseconds.
     * @param needed the number of sessions that completes a word.
     * @return a flow of the chapter, or null if it does not exist.
     */
    @Query(SqlFragments.CHAPTER_SUMMARY + " WHERE h.id = :id")
    fun chapterSummary(id: Long, now: Long, needed: Int): Flow<ChapterSummary?>
}
