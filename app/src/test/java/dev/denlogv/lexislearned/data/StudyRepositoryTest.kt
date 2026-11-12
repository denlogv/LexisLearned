package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.data.SessionShape.Capped
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
        assertTrue(study.sessionCards(id, null, null, 1, Capped(10, 5)).none { it.id == first.id })
        now += DAY
        val session = study.sessionCards(id, null, null, 1, Capped(10, 5))
        assertEquals(first.id, session.first().id)
        assertEquals(6, session.size)
        assertEquals(2, study.sessionCards(id, null, null, 1, Capped(2, 5)).size)
        assertEquals(1, study.sessionCards(id, null, null, 1, Capped(10, 0)).size)
    }

    @Test
    fun sessionCardsCanBeLimitedToAChapterOrPart() = runBlocking {
        val id = storage.import(partedDeck())
        val chapter = library.chapters(id, 1).first()[0]
        assertEquals(4, study.sessionCards(id, chapter.id, null, 1, Capped(10, 10)).size)
        val part = library.parts(id).first().single()
        assertEquals(8, study.sessionCards(id, null, part.id, 1, Capped(10, 10)).size)
    }

    @Test
    fun aCappedSessionNeverExceedsItsNumbersEvenWhenNothingIsDue() = runBlocking {
        val id = storage.import(sampleDeck())
        assertEquals(3, study.sessionCards(id, null, null, 1, Capped(3, 3)).size)
        assertEquals(0, study.sessionCards(id, null, null, 1, Capped(3, 0)).size)
        assertEquals(0, study.sessionCards(id, null, null, 1, Capped(0, 5)).size)
    }

    @Test
    fun remainingSessionsSumWhatEachWordStillLacks() = runBlocking {
        val id = storage.import(partedDeck())
        val chapter = library.chapters(id, 3).first()[0]
        val part = library.parts(id).first().single()
        assertEquals(12, study.remainingSessions(id, null, null, 1))
        assertEquals(36, study.remainingSessions(id, null, null, 3))
        assertEquals(12, study.remainingSessions(id, chapter.id, null, 3))
        assertEquals(24, study.remainingSessions(id, null, part.id, 3))
        val (a, b) = cardsOf(id)
        study.finishSession(a, Grade.GOOD, spaced = true)
        study.finishSession(b, Grade.AGAIN, spaced = true) // a failed session does not count
        assertEquals(11, study.remainingSessions(id, null, null, 1))
        assertEquals(35, study.remainingSessions(id, null, null, 3))
        assertEquals(0, study.remainingSessions(id, chapter.id, null, 0))
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

    @Test
    fun learnedFillersAreOnlyStudiedWordsFromTheChapterFirstThenTheDeck() = runBlocking {
        val deck = Deck(
            "d",
            "T",
            chapters = listOf(
                Chapter("c1", "One", cards = listOf(simpleCard("a"), simpleCard("b"), simpleCard("c"))),
                Chapter("c2", "Two", cards = listOf(simpleCard("d"), simpleCard("e"))),
            ),
        )
        val cards = cardsOf(storage.import(deck))
        fun card(source: String) = cards.first { it.sourceId == source }
        val a = card("a")
        assertTrue(study.learnedFillers(a, listOf(a.id), 3).isEmpty())
        study.finishSession(card("b"), Grade.GOOD, spaced = true)
        study.finishSession(card("d"), Grade.GOOD, spaced = true)
        assertEquals(listOf("b"), study.learnedFillers(a, listOf(a.id), 1).map { it.sourceId })
        assertEquals(setOf("b", "d"), study.learnedFillers(a, listOf(a.id), 5).map { it.sourceId }.toSet())
        assertTrue(study.learnedFillers(a, listOf(a.id, card("b").id, card("d").id), 5).isEmpty())
        assertEquals(listOf("b"), study.learnedFillers(a, emptyList(), 1).map { it.sourceId })
    }

    @Test
    fun resumableCardsAreTheOnesThatStillExistAndAreNotCompleted() = runBlocking {
        val id = storage.import(sampleDeck())
        val all = cardsOf(id)
        val done = study.finishSession(all[0], Grade.GOOD, spaced = true)
        val ids = listOf(done.id, all[1].id, all[2].id, 99_999L)
        assertEquals(listOf(all[1].id, all[2].id), study.resumableCards(ids, 1).map { it.id })
        assertEquals(listOf(all[0].id, all[1].id, all[2].id), study.resumableCards(ids, 2).map { it.id })
        assertTrue(study.resumableCards(emptyList(), 1).isEmpty())
    }

    @Test
    fun resumableCardsHaveTheirCurrentProgress() = runBlocking {
        val id = storage.import(sampleDeck())
        val card = cardsOf(id).first()
        study.finishSession(card, Grade.AGAIN, spaced = true)
        library.resetCard(card.id)
        val loaded = study.resumableCards(listOf(card.id), 1).single()
        assertEquals(0, loaded.reps)
        assertEquals(null, loaded.dueAt)
        assertEquals(0, loaded.sessionsDone)
    }

    @Test
    fun resumableCardsOfADeletedDeckAreGone() = runBlocking {
        val id = storage.import(sampleDeck())
        val ids = cardsOf(id).map { it.id }
        library.delete(id)
        assertTrue(study.resumableCards(ids, 1).isEmpty())
    }

    @Test
    fun resumableCardsWorkForMoreIdsThanOneQueryTakes() = runBlocking {
        val id = storage.import(sampleDeck())
        val real = cardsOf(id).map { it.id }
        val ids = real.take(2) + (100_000L..101_400L) + real.drop(2)
        assertEquals(real, study.resumableCards(ids, 1).map { it.id })
    }
}
