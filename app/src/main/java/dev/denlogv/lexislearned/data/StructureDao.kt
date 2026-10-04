package dev.denlogv.lexislearned.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Queries for the structure of decks: decks, parts and chapters. */
@Dao
interface StructureDao {
    /**
     * Observes one deck.
     *
     * @param id the deck's database id.
     * @return a flow of the deck, or null if it does not exist.
     */
    @Query("SELECT * FROM decks WHERE id = :id")
    fun deckFlow(id: Long): Flow<DeckEntity?>

    /**
     * Loads one deck.
     *
     * @param id the deck's database id.
     * @return the deck, or null if it does not exist.
     */
    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun deck(id: Long): DeckEntity?

    /**
     * Finds a deck by the id it had in its source file.
     *
     * @param sourceId the source id.
     * @return the deck, or null if none was imported with that id.
     */
    @Query("SELECT * FROM decks WHERE sourceId = :sourceId LIMIT 1")
    suspend fun deckBySource(sourceId: String): DeckEntity?

    /**
     * Observes the parts of a deck.
     *
     * @param deckId the deck.
     * @return a flow of the parts in order.
     */
    @Query("SELECT * FROM parts WHERE deckId = :deckId ORDER BY id")
    fun parts(deckId: Long): Flow<List<PartEntity>>

    /**
     * Loads the parts of a deck.
     *
     * @param deckId the deck.
     * @return the parts in order.
     */
    @Query("SELECT * FROM parts WHERE deckId = :deckId ORDER BY id")
    suspend fun partsList(deckId: Long): List<PartEntity>

    /**
     * Finds a part of a deck by its id in the source.
     *
     * @param deckId the deck.
     * @param sourceId the part's id in its source file.
     * @return the part, or null if the deck has none with that id.
     */
    @Query("SELECT * FROM parts WHERE deckId = :deckId AND sourceId = :sourceId LIMIT 1")
    suspend fun partBySource(deckId: Long, sourceId: String): PartEntity?

    /**
     * Observes one part.
     *
     * @param id the part's database id.
     * @return a flow of the part, or null if it does not exist.
     */
    @Query("SELECT * FROM parts WHERE id = :id")
    fun partFlow(id: Long): Flow<PartEntity?>

    /**
     * Observes one chapter.
     *
     * @param id the chapter's database id.
     * @return a flow of the chapter, or null if it does not exist.
     */
    @Query("SELECT * FROM chapters WHERE id = :id")
    fun chapterFlow(id: Long): Flow<ChapterEntity?>

    /**
     * Loads the chapters of a deck.
     *
     * @param deckId the deck.
     * @return the chapters in reading order.
     */
    @Query("SELECT * FROM chapters WHERE deckId = :deckId ORDER BY id")
    suspend fun chapters(deckId: Long): List<ChapterEntity>

    /**
     * Inserts a deck.
     *
     * @param deck the deck row (id 0 to let the database assign one).
     * @return the new database id.
     */
    @Insert
    suspend fun insertDeck(deck: DeckEntity): Long

    /**
     * Inserts a part.
     *
     * @param part the part row.
     * @return the new database id.
     */
    @Insert
    suspend fun insertPart(part: PartEntity): Long

    /**
     * Inserts a chapter.
     *
     * @param chapter the chapter row.
     * @return the new database id.
     */
    @Insert
    suspend fun insertChapter(chapter: ChapterEntity): Long

    /**
     * Deletes a deck with all its parts, chapters and cards.
     *
     * @param id the deck's database id.
     */
    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteDeck(id: Long)
}
