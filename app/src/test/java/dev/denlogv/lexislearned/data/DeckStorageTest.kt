package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.sampleDeck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeckStorageTest {
    private lateinit var db: AppDatabase
    private lateinit var storage: DeckStorage
    private lateinit var library: DeckLibrary
    private lateinit var study: StudyRepository

    @Before
    fun open() {
        db = memoryDb()
        storage = DeckStorage(db)
        library = DeckLibrary(db)
        study = StudyRepository(db)
    }

    @After
    fun close() = db.close()

    private suspend fun cardsOf(deckId: Long) = db.cardDao().cards(deckId)

    @Test
    fun importThenExportGivesTheSameDeck() = runBlocking {
        val id = storage.import(partedDeck())
        assertEquals(partedDeck(), storage.export(id))
    }

    @Test
    fun exportOfUnknownDeckFails() {
        assertThrows(IllegalStateException::class.java) { runBlocking { storage.export(42) } }
    }

    @Test
    fun importingAgainReplacesTheDeckButKeepsProgress() = runBlocking {
        val id = storage.import(sampleDeck())
        val card = cardsOf(id).first()
        study.finishSession(card, Grade.GOOD, spaced = true)
        val again = storage.import(sampleDeck().copy(title = "Renamed"))
        assertEquals(1, library.decks(1).first().size)
        assertEquals("Renamed", library.deck(again).first()?.title)
        val kept = cardsOf(again).first { it.sourceId == card.sourceId }
        assertEquals(1, kept.sessionsDone)
        assertNotNull(kept.dueAt)
        assertEquals(0, cardsOf(again).count { it.sessionsDone > 0 } - 1)
    }
}
