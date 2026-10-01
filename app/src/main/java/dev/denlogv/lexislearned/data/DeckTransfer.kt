package dev.denlogv.lexislearned.data

import androidx.room.withTransaction
import dev.denlogv.lexislearned.domain.Card
import dev.denlogv.lexislearned.domain.CardProgress
import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.domain.PartRef
import dev.denlogv.lexislearned.domain.Side

/**
 * Stores a [Deck] in the database. Importing a deck that was imported before replaces it but keeps the study progress
 * of the cards that are still in it.
 *
 * @param db the database.
 */
internal class DeckImporter(private val db: AppDatabase) {
    private val structure = db.structureDao()
    private val cards = db.cardDao()

    /**
     * Imports a deck in one transaction.
     *
     * @param deck the deck to store.
     * @return the database id of the stored deck.
     */
    suspend fun import(deck: Deck): Long = db.withTransaction {
        val kept = replaceExisting(deck.id)
        val deckId = structure.insertDeck(DeckEntity(0, deck.id, deck.title, deck.nativeTitle, deck.frontLang, deck.backLang))
        val partIds = HashMap<String, Long>()
        deck.chapters.forEach { importChapter(deckId, it, partIds, kept) }
        deckId
    }

    /**
     * Deletes an earlier import of the same source deck, remembering its progress.
     *
     * @param sourceId the deck's id in its source file.
     * @return the study state of the old cards by source id; empty if the deck was not imported before.
     */
    private suspend fun replaceExisting(sourceId: String): Map<String, ProgressRow> {
        val existing = structure.deckBySource(sourceId) ?: return emptyMap()
        val kept = cards.progressRows(existing.id).associateBy { it.sourceId }
        structure.deleteDeck(existing.id)
        return kept
    }

    /**
     * Stores a chapter with its cards, creating its part the first time it is seen.
     *
     * @param deckId the deck's database id.
     * @param chapter the chapter.
     * @param partIds database ids of the parts created so far, by source id; updated here.
     * @param kept progress to restore, by card source id.
     */
    private suspend fun importChapter(deckId: Long, chapter: Chapter, partIds: MutableMap<String, Long>, kept: Map<String, ProgressRow>) {
        val partId = chapter.part?.let { partIdFor(deckId, it, partIds) }
        val chapterId = structure.insertChapter(ChapterEntity(0, deckId, chapter.id, chapter.title, chapter.nativeTitle, partId))
        cards.insertCards(chapter.cards.map { it.toEntity(deckId, chapterId, kept[it.id]) })
    }

    /**
     * The database id of a part, inserting it if it is new.
     *
     * @param deckId the deck's database id.
     * @param part the part.
     * @param partIds database ids of the parts created so far, by source id; updated here.
     * @return the part's database id.
     */
    private suspend fun partIdFor(deckId: Long, part: PartRef, partIds: MutableMap<String, Long>): Long = partIds[part.id]
        ?: structure.insertPart(PartEntity(0, deckId, part.id, part.title, part.nativeTitle)).also { partIds[part.id] = it }

    /**
     * Converts a card to its database row, preferring the progress kept from an earlier import.
     *
     * @receiver the card.
     * @param deckId the deck's database id.
     * @param chapterId the chapter's database id.
     * @param kept the study state from an earlier import, if any.
     * @return the row to insert.
     */
    private fun Card.toEntity(deckId: Long, chapterId: Long, kept: ProgressRow?): CardEntity {
        val p = kept?.let { CardProgress(it.reps, it.ease, it.intervalDays, it.dueAt, it.lapses, it.sessionsDone) } ?: progress
        return CardEntity(
            deckId = deckId,
            chapterId = chapterId,
            sourceId = id,
            frontText = front.text,
            frontTranscription = front.transcription,
            frontExample = front.example,
            frontGender = front.gender,
            backText = back.text,
            backExample = back.example,
            backGender = back.gender,
            reps = p.reps,
            ease = p.ease,
            intervalDays = p.intervalDays,
            dueAt = p.dueAt,
            lapses = p.lapses,
            sessionsDone = p.sessionsDone,
        )
    }
}

/**
 * Reads a stored deck back as a [Deck], including study progress.
 *
 * @param db the database.
 */
internal class DeckExporter(db: AppDatabase) {
    private val structure = db.structureDao()
    private val cards = db.cardDao()

    /**
     * Loads a deck with its parts, chapters, cards and progress.
     *
     * @param deckId the deck's database id.
     * @return the deck.
     * @throws IllegalStateException if there is no such deck.
     */
    suspend fun export(deckId: Long): Deck {
        val deck = structure.deck(deckId) ?: error("Deck $deckId not found")
        val cardsByChapter = cards.cards(deckId).groupBy { it.chapterId }
        val parts = structure.partsList(deckId).associate { it.id to PartRef(it.sourceId, it.title, it.nativeTitle) }
        return Deck(
            id = deck.sourceId,
            title = deck.title,
            nativeTitle = deck.nativeTitle,
            frontLang = deck.frontLang,
            backLang = deck.backLang,
            chapters = structure.chapters(deckId).map { chapter ->
                Chapter(
                    chapter.sourceId,
                    chapter.title,
                    chapter.nativeTitle,
                    cards = cardsByChapter[chapter.id].orEmpty().map { it.toCard() },
                    part = chapter.partId?.let { parts[it] },
                )
            },
        )
    }

    /**
     * Converts a database row to a card.
     *
     * @receiver the stored card.
     * @return the card with its progress.
     */
    private fun CardEntity.toCard(): Card = Card(
        sourceId,
        Side(frontText, frontTranscription, frontExample, frontGender),
        Side(backText, null, backExample, backGender),
        progress,
    )
}
