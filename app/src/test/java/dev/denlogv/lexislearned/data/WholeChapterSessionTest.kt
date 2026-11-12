package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck
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

/** The sessions that take every studyable word of one chapter instead of a fixed number of words. */
@RunWith(RobolectricTestRunner::class)
class WholeChapterSessionTest {
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

    private suspend fun whole(deck: Long, chapter: Long? = null, part: Long? = null, needed: Int = 1) =
        study.sessionCards(deck, chapter, part, needed, SessionShape.WholeChapter)

    private suspend fun studyAll(cards: List<CardEntity>, grade: Grade = Grade.GOOD) = cards.forEach {
        study.finishSession(it, grade, true)
    }

    @Test
    fun aChapterSessionHoldsEveryWordOfThatChapter() = runBlocking {
        val id = storage.import(sampleDeck())
        val byChapter = cardsOf(id).groupBy { it.chapterId }
        val (first, second) = byChapter.keys.sorted().take(2)
        assertEquals(byChapter.getValue(second).map { it.id }, whole(id, chapter = second).map { it.id })
        assertEquals(byChapter.getValue(first).map { it.id }, whole(id, chapter = first).map { it.id })
    }

    @Test
    fun aBookSessionTakesTheFirstChapterAndTheNextOneContinuesAfterIt() = runBlocking {
        val id = storage.import(sampleDeck())
        val byChapter = cardsOf(id).groupBy { it.chapterId }
        val chapters = byChapter.keys.sorted()
        val firstSession = whole(id)
        assertEquals(byChapter.getValue(chapters[0]).map { it.id }, firstSession.map { it.id })
        studyAll(firstSession)
        assertEquals(byChapter.getValue(chapters[1]).map { it.id }, whole(id).map { it.id })
        studyAll(whole(id))
        assertEquals(byChapter.getValue(chapters[2]).map { it.id }, whole(id).map { it.id })
    }

    @Test
    fun aPartSessionTakesTheFirstChapterOfThePartEvenWhenAnEarlierChapterOutsideItHasWords() = runBlocking {
        val id = storage.import(partedDeck())
        val part = library.parts(id).first().single()
        val chapterIds = cardsOf(id).map { it.chapterId }.distinct().sorted()
        val session = whole(id, part = part.id)
        assertEquals(setOf(chapterIds[1]), session.map { it.chapterId }.toSet())
        assertEquals(4, session.size)
        studyAll(session)
        assertEquals(setOf(chapterIds[2]), whole(id, part = part.id).map { it.chapterId }.toSet())
    }

    @Test
    fun aChapterSessionPutsDueWordsBeforeNewOnesAndLeavesOtherChaptersOut() = runBlocking {
        val id = storage.import(sampleDeck())
        val byChapter = cardsOf(id).groupBy { it.chapterId }
        val chapters = byChapter.keys.sorted()
        val dueWord = byChapter.getValue(chapters[0])[2]
        study.finishSession(dueWord, Grade.AGAIN, spaced = true)
        study.finishSession(byChapter.getValue(chapters[1])[0], Grade.AGAIN, spaced = true)
        now += DAY
        val session = whole(id)
        assertEquals(dueWord.id, session.first().id)
        assertEquals(4, session.size)
        assertEquals(setOf(chapters[0]), session.map { it.chapterId }.toSet())
        assertEquals(listOf(dueWord.id), session.filter { it.dueAt != null }.map { it.id })
    }

    @Test
    fun wordsThatAreNotDueYetOrAreCompletedDoNotCountAsStudyable() = runBlocking {
        val id = storage.import(sampleDeck())
        val chapters = cardsOf(id).groupBy { it.chapterId }.toSortedMap()
        studyAll(chapters.values.first())
        assertEquals(chapters.keys.toList()[1], whole(id).first().chapterId) // the first chapter waits for tomorrow
        now += DAY
        assertEquals(chapters.keys.first(), whole(id, needed = 2).first().chapterId) // due and 1 of 2 sessions done
        assertEquals(chapters.keys.toList()[1], whole(id, needed = 1).first().chapterId) // completed words are skipped
    }

    @Test
    fun aScopeWithNothingToStudyGivesNoSession() = runBlocking {
        val id = storage.import(partedDeck())
        val part = library.parts(id).first().single()
        studyAll(cardsOf(id))
        assertTrue(whole(id).isEmpty())
        assertTrue(whole(id, part = part.id).isEmpty())
        assertTrue(whole(id, chapter = cardsOf(id).first().chapterId).isEmpty())
        assertTrue(whole(id + 1).isEmpty())
    }

    @Test
    fun aChapterWithHundredsOfWordsIsOneSessionWithTheDueWordsFirst() = runBlocking {
        val deck = Deck("big", "Big", chapters = listOf(Chapter("c1", "Huge", cards = (1..500).map { simpleCard("w$it") })))
        val id = storage.import(deck)
        val all = cardsOf(id)
        studyAll(all.take(120), Grade.AGAIN)
        now += DAY
        val session = whole(id, chapter = all.first().chapterId)
        assertEquals(500, session.size)
        assertEquals(all.size, session.map { it.id }.toSet().size)
        assertEquals(all.take(120).map { it.id }.toSet(), session.take(120).map { it.id }.toSet())
        assertEquals(all.drop(120).map { it.id }, session.drop(120).map { it.id })
    }

    @Test
    fun theChapterFinderAgreesWithTheCardQueriesInEveryState() = runBlocking {
        val id = storage.import(partedDeck())
        val cards = cardsOf(id)
        val dao = db.cardDao()
        fun both() = runBlocking {
            val found = dao.firstStudyableChapter(id, -1, 1, now)
            val any = dao.dueCards(id, -1, -1, 1, now, 1) + dao.newCards(id, -1, -1, 1)
            assertEquals(any.isNotEmpty(), found != null)
        }
        both()
        studyAll(cards.take(8), Grade.AGAIN)
        both()
        now += DAY
        both()
        studyAll(cards, Grade.GOOD)
        both()
        now += 30 * DAY
        both() // everything is completed, so nothing is due again
        assertNull(dao.firstStudyableChapter(id, -1, 1, now))
    }
}
