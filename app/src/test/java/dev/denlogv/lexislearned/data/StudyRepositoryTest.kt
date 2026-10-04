package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.sampleDeck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudyRepositoryTest {
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
    fun sessionCardsTakeDueWordsFirstThenNewOnesUpToTheLimits() = runBlocking {
        val id = storage.import(sampleDeck())
        val first = cardsOf(id).first()
        study.finishSession(first, Grade.AGAIN, spaced = true) // due in ten minutes
        assertTrue(study.sessionCards(id, null, null, 1, 10, 5).none { it.id == first.id })
        now += DAY
        val session = study.sessionCards(id, null, null, 1, 10, 5)
        assertEquals(first.id, session.first().id)
        assertEquals(6, session.size)
        assertEquals(2, study.sessionCards(id, null, null, 1, 2, 5).size)
        assertEquals(1, study.sessionCards(id, null, null, 1, 10, 0).size)
    }

    @Test
    fun sessionCardsCanBeLimitedToAChapterOrPart() = runBlocking {
        val id = storage.import(partedDeck())
        val chapter = library.chapters(id, 1).first()[0]
        assertEquals(4, study.sessionCards(id, chapter.id, null, 1, 10, 10).size)
        val part = library.parts(id).first().single()
        assertEquals(8, study.sessionCards(id, null, part.id, 1, 10, 10).size)
    }

    @Test
    fun finishSessionSchedulesAndCountsSessions() = runBlocking {
        val id = storage.import(sampleDeck())
        val (a, b, c) = cardsOf(id)
        val good = study.finishSession(a, Grade.GOOD, spaced = true)
        assertEquals(1, good.sessionsDone)
        assertEquals(now + DAY, good.dueAt)
        val failed = study.finishSession(b, Grade.AGAIN, spaced = true)
        assertEquals(0, failed.sessionsDone)
        assertEquals(now + 600_000, failed.dueAt)
        val unspaced = study.finishSession(c, Grade.EASY, spaced = false)
        assertEquals(now, unspaced.dueAt)
        assertEquals(unspaced.sessionsDone, cardsOf(id).first { it.id == c.id }.sessionsDone)
    }

    @Test
    fun decoysComeFromTheSameChapterFirstThenFromTheDeck() = runBlocking {
        val deck = Deck(
            "d",
            "T",
            chapters = listOf(
                Chapter("c1", "One", cards = listOf(simpleCard("a"), simpleCard("b"))),
                Chapter("c2", "Two", cards = listOf(simpleCard("c"), simpleCard("d"), simpleCard("e"))),
            ),
        )
        val cards = cardsOf(storage.import(deck))
        val a = cards.first { it.sourceId == "a" }
        val same = study.decoys(a, listOf(a.id), 1)
        assertEquals(listOf("b"), same.map { it.sourceId })
        val more = study.distractors(a, 3)
        assertEquals(3, more.size)
        assertTrue(more.none { it.id == a.id })
        assertEquals("b", more.first().sourceId)
        assertTrue(study.decoys(a, emptyList(), 1).isNotEmpty())
    }
}
