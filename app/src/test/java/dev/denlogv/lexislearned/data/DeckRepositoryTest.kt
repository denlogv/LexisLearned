package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Card
import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.PartRef
import dev.denlogv.lexislearned.domain.Side
import dev.denlogv.lexislearned.sampleDeck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeckRepositoryTest {
    private val day = 24 * 60 * 60_000L
    private var now = 1_000_000L
    private lateinit var db: AppDatabase
    private lateinit var repo: DeckRepository

    @Before
    fun open() {
        db = memoryDb()
        repo = DeckRepository(db, clock = { now })
    }

    @After
    fun close() = db.close()

    private fun parted(): Deck {
        val part = PartRef("p1", "Part One", "Часть первая")
        return sampleDeck().let { d -> d.copy(chapters = d.chapters.mapIndexed { i, c -> if (i > 0) c.copy(part = part) else c }) }
    }

    private suspend fun cardsOf(deckId: Long) = db.cardDao().cards(deckId)

    @Test
    fun importThenExportGivesTheSameDeck() = runBlocking {
        val id = repo.import(parted())
        assertEquals(parted(), repo.export(id))
    }

    @Test
    fun exportOfUnknownDeckFails() {
        assertThrows(IllegalStateException::class.java) { runBlocking { repo.export(42) } }
    }

    @Test
    fun importingAgainReplacesTheDeckButKeepsProgress() = runBlocking {
        val id = repo.import(sampleDeck())
        val card = cardsOf(id).first()
        repo.finishSession(card, Grade.GOOD, spaced = true)
        val again = repo.import(sampleDeck().copy(title = "Renamed"))
        assertEquals(1, repo.decks(1).first().size)
        assertEquals("Renamed", repo.deck(again).first()?.title)
        val kept = cardsOf(again).first { it.sourceId == card.sourceId }
        assertEquals(1, kept.sessionsDone)
        assertNotNull(kept.dueAt)
        assertEquals(0, cardsOf(again).count { it.sessionsDone > 0 } - 1)
    }

    @Test
    fun sessionCardsTakeDueWordsFirstThenNewOnesUpToTheLimits() = runBlocking {
        val id = repo.import(sampleDeck())
        val first = cardsOf(id).first()
        repo.finishSession(first, Grade.AGAIN, spaced = true) // due in ten minutes
        assertTrue(repo.sessionCards(id, null, null, 1, 10, 5).none { it.id == first.id })
        now += day
        val session = repo.sessionCards(id, null, null, 1, 10, 5)
        assertEquals(first.id, session.first().id)
        assertEquals(6, session.size)
        assertEquals(2, repo.sessionCards(id, null, null, 1, 2, 5).size)
        assertEquals(1, repo.sessionCards(id, null, null, 1, 10, 0).size)
    }

    @Test
    fun sessionCardsCanBeLimitedToAChapterOrPart() = runBlocking {
        val id = repo.import(parted())
        val chapter = repo.chapters(id, 1).first()[0]
        assertEquals(4, repo.sessionCards(id, chapter.id, null, 1, 10, 10).size)
        val part = repo.parts(id).first().single()
        assertEquals(8, repo.sessionCards(id, null, part.id, 1, 10, 10).size)
    }

    @Test
    fun finishSessionSchedulesAndCountsSessions() = runBlocking {
        val id = repo.import(sampleDeck())
        val (a, b, c) = cardsOf(id)
        val good = repo.finishSession(a, Grade.GOOD, spaced = true)
        assertEquals(1, good.sessionsDone)
        assertEquals(now + day, good.dueAt)
        val failed = repo.finishSession(b, Grade.AGAIN, spaced = true)
        assertEquals(0, failed.sessionsDone)
        assertEquals(now + 600_000, failed.dueAt)
        val unspaced = repo.finishSession(c, Grade.EASY, spaced = false)
        assertEquals(now, unspaced.dueAt)
        assertEquals(unspaced.sessionsDone, cardsOf(id).first { it.id == c.id }.sessionsDone)
    }

    @Test
    fun resetsClearProgressAtEveryLevel() = runBlocking {
        val id = repo.import(parted())
        suspend fun studyAll() = cardsOf(id).forEach { repo.finishSession(it, Grade.GOOD, true) }

        studyAll()
        repo.resetCard(cardsOf(id).first().id)
        assertEquals(11, cardsOf(id).count { it.sessionsDone > 0 })
        studyAll()
        repo.resetChapter(repo.chapters(id, 1).first()[0].id)
        assertEquals(8, cardsOf(id).count { it.sessionsDone > 0 })
        studyAll()
        repo.resetPart(repo.parts(id).first().single().id)
        assertEquals(4, cardsOf(id).count { it.sessionsDone > 0 })
        repo.resetProgress(id)
        assertEquals(0, cardsOf(id).count { it.sessionsDone > 0 || it.dueAt != null })
    }

    @Test
    fun deletingADeckRemovesEverythingBelowIt() = runBlocking {
        val id = repo.import(parted())
        repo.delete(id)
        assertNull(repo.deck(id).first())
        assertTrue(cardsOf(id).isEmpty())
        assertTrue(repo.parts(id).first().isEmpty())
    }

    @Test
    fun summariesCountCompletedInProgressAndDue() = runBlocking {
        val id = repo.import(sampleDeck())
        val cards = cardsOf(id)
        repo.finishSession(repo.finishSession(cards[0], Grade.GOOD, true), Grade.GOOD, true)
        repo.finishSession(cards[1], Grade.GOOD, true)
        repo.finishSession(cards[2], Grade.AGAIN, true)
        now += day
        val deck = repo.decks(2).first().single()
        assertEquals(12, deck.total)
        assertEquals(1, deck.completed)
        assertEquals(1, deck.inProgress)
        assertEquals(3, deck.units)
        assertEquals(2, deck.due)
        assertEquals(3f / 24f, deck.progress, 0.0001f)
        val chapter = repo.chapterSummary(repo.chapters(id, 2).first()[0].id, 2).first()
        assertEquals(4, chapter?.total)
        assertEquals("Chapter 1", repo.chapter(chapter!!.id).first()?.title)
        assertEquals(4, repo.cardsOfChapter(chapter.id).first().size)
    }

    @Test
    fun decoysComeFromTheSameChapterFirstThenFromTheDeck() = runBlocking {
        val deck = Deck(
            "d",
            "T",
            chapters = listOf(
                Chapter("c1", "One", cards = listOf(card("a"), card("b"))),
                Chapter("c2", "Two", cards = listOf(card("c"), card("d"), card("e"))),
            ),
        )
        val id = repo.import(deck)
        val cards = cardsOf(id)
        val a = cards.first { it.sourceId == "a" }
        val same = repo.decoys(a, listOf(a.id), 1)
        assertEquals(listOf("b"), same.map { it.sourceId })
        val more = repo.distractors(a, 3)
        assertEquals(3, more.size)
        assertTrue(more.none { it.id == a.id })
        assertEquals("b", more.first().sourceId)
        assertTrue(repo.decoys(a, emptyList(), 1).isNotEmpty())
    }

    private fun card(id: String) = Card(id, Side("w-$id"), Side("t-$id"))
}
