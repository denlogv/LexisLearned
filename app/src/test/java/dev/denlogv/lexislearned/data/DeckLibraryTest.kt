package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.sampleDeck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeckLibraryTest {
    private var now = 1_000_000L
    private lateinit var db: AppDatabase
    private lateinit var storage: DeckStorage
    private lateinit var library: DeckLibrary
    private lateinit var study: StudyRepository

    @Before
    fun open() {
        db = memoryDb()
        storage = DeckStorage(db)
        library = DeckLibrary(db, clock = { now })
        study = StudyRepository(db, clock = { now })
    }

    @After
    fun close() = db.close()

    private suspend fun cardsOf(deckId: Long) = db.cardDao().cards(deckId)

    @Test
    fun resetsClearProgressAtEveryLevel() = runBlocking {
        val id = storage.import(partedDeck())
        suspend fun studyAll() = cardsOf(id).forEach { study.finishSession(it, Grade.GOOD, true) }

        studyAll()
        library.resetCard(cardsOf(id).first().id)
        assertEquals(11, cardsOf(id).count { it.sessionsDone > 0 })
        studyAll()
        library.resetChapter(library.chapters(id, 1).first()[0].id)
        assertEquals(8, cardsOf(id).count { it.sessionsDone > 0 })
        studyAll()
        library.resetPart(library.parts(id).first().single().id)
        assertEquals(4, cardsOf(id).count { it.sessionsDone > 0 })
        library.resetProgress(id)
        assertEquals(0, cardsOf(id).count { it.sessionsDone > 0 || it.dueAt != null })
    }

    @Test
    fun deletingADeckRemovesEverythingBelowIt() = runBlocking {
        val id = storage.import(partedDeck())
        library.delete(id)
        assertNull(library.deck(id).first())
        assertTrue(cardsOf(id).isEmpty())
        assertTrue(library.parts(id).first().isEmpty())
    }

    @Test
    fun summariesCountCompletedInProgressAndDue() = runBlocking {
        val id = storage.import(sampleDeck())
        val cards = cardsOf(id)
        study.finishSession(study.finishSession(cards[0], Grade.GOOD, true), Grade.GOOD, true)
        study.finishSession(cards[1], Grade.GOOD, true)
        study.finishSession(cards[2], Grade.AGAIN, true)
        now += DAY
        val deck = library.decks(2).first().single()
        assertEquals(12, deck.total)
        assertEquals(1, deck.completed)
        assertEquals(1, deck.inProgress)
        assertEquals(3, deck.units)
        assertEquals(2, deck.due)
        assertEquals(3f / 24f, deck.progress, 0.0001f)
        val chapter = library.chapterSummary(library.chapters(id, 2).first()[0].id, 2).first()
        assertEquals(4, chapter?.total)
        assertEquals("Chapter 1", library.chapter(chapter!!.id).first()?.title)
        assertEquals(4, library.cardsOfChapter(chapter.id).first().size)
    }
}
