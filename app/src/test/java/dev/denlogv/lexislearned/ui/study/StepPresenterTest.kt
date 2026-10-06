package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode
import dev.denlogv.lexislearned.sampleDeck
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StepPresenterTest {
    private val db = memoryDb()
    private val storage = DeckStorage(db)
    private val study = StudyRepository(db)

    private suspend fun cards(): Map<Long, CardEntity> = storage.import(sampleDeck()).let { db.cardDao().cards(it) }.associateBy { it.id }

    @Test
    fun selectStepsGetThreeWrongAnswersAndTheRightOne() = runBlocking {
        val cards = cards()
        val card = cards.values.first()
        val shown = StepPresenter(
            study,
            Direction.FORWARD,
            Random(1),
        ).present(SessionPlanner.Step(StudyMode.SELECT, listOf(card.id)), cards)
        assertEquals(4, shown.options.size)
        assertTrue(card.backText in shown.options)
        assertEquals(shown.options.distinct(), shown.options)
        assertTrue(shown.decoyIds.isEmpty())
    }

    @Test
    fun smallPairBoardsAreFilledUpToThePairSize() = runBlocking {
        val cards = cards()
        val two = cards.values.take(2)
        cards.values.drop(2).forEach { study.finishSession(it, Grade.GOOD, spaced = true) }
        val shown = StepPresenter(study, Direction.REVERSE, Random(1)).present(
            SessionPlanner.Step(
                StudyMode.PAIR,
                two.map {
                    it.id
                },
            ),
            cards,
        )
        assertEquals(6, shown.cards.size)
        assertEquals(4, shown.decoyIds.size)
        assertTrue(shown.options.isEmpty())
        assertTrue(shown.cards.map { it.answer(Direction.REVERSE) }.let { it.distinct().size == it.size })
    }

    @Test
    fun aLargerPairSizeGivesALargerBoard() = runBlocking {
        val cards = cards()
        val two = cards.values.take(2)
        cards.values.drop(2).forEach { study.finishSession(it, Grade.GOOD, spaced = true) }
        val shown = StepPresenter(study, Direction.FORWARD, Random(1), pairSize = 9).present(
            SessionPlanner.Step(StudyMode.PAIR, two.map { it.id }),
            cards,
        )
        assertEquals(9, shown.cards.size)
        assertEquals(7, shown.decoyIds.size)
    }

    @Test
    fun pairFillersAreNeverWordsThatWereNotStudiedYet() = runBlocking {
        val cards = cards()
        val studied = cards.values.drop(2).take(3).onEach { study.finishSession(it, Grade.GOOD, spaced = true) }
        val two = cards.values.take(2)
        val shown = StepPresenter(study, Direction.FORWARD, Random(1)).present(
            SessionPlanner.Step(StudyMode.PAIR, two.map { it.id }),
            cards,
        )
        assertEquals(studied.map { it.id }.toSet(), shown.decoyIds)
    }

    @Test
    fun boardsAreNotFilledBeyondTheChosenPairSize() = runBlocking {
        val cards = cards()
        cards.values.drop(3).forEach { study.finishSession(it, Grade.GOOD, spaced = true) }
        val three = cards.values.take(3)
        val shown = StepPresenter(study, Direction.FORWARD, Random(1), pairSize = 3).present(
            SessionPlanner.Step(StudyMode.PAIR, three.map { it.id }),
            cards,
        )
        assertEquals(three, shown.cards)
    }

    @Test
    fun pairBoardStaysSmallWhenNothingWasStudiedYet() = runBlocking {
        val cards = cards()
        val two = cards.values.take(2)
        val shown = StepPresenter(study, Direction.FORWARD, Random(1)).present(
            SessionPlanner.Step(StudyMode.PAIR, two.map { it.id }),
            cards,
        )
        assertEquals(two, shown.cards)
        assertTrue(shown.decoyIds.isEmpty())
    }

    @Test
    fun otherModesShowJustTheirCards() = runBlocking {
        val cards = cards()
        val card = cards.values.first()
        val shown = StepPresenter(study, Direction.FORWARD, Random(1)).present(SessionPlanner.Step(StudyMode.TYPE, listOf(card.id)), cards)
        assertEquals(listOf(card), shown.cards)
        assertTrue(shown.options.isEmpty() && shown.decoyIds.isEmpty())
    }
}
