package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.DeckRepository
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.domain.Direction
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
    private val repo = DeckRepository(db)

    private suspend fun cards(): Map<Long, CardEntity> = repo.import(sampleDeck()).let { db.cardDao().cards(it) }.associateBy { it.id }

    @Test
    fun selectStepsGetThreeWrongAnswersAndTheRightOne() = runBlocking {
        val cards = cards()
        val card = cards.values.first()
        val shown = StepPresenter(repo, Direction.FORWARD, Random(1)).present(SessionPlanner.Step(StudyMode.SELECT, listOf(card.id)), cards)
        assertEquals(4, shown.options.size)
        assertTrue(card.backText in shown.options)
        assertEquals(shown.options.distinct(), shown.options)
        assertTrue(shown.decoyIds.isEmpty())
    }

    @Test
    fun smallPairBoardsArePaddedWithFillers() = runBlocking {
        val cards = cards()
        val two = cards.values.take(2)
        val shown = StepPresenter(repo, Direction.REVERSE, Random(1)).present(SessionPlanner.Step(StudyMode.PAIR, two.map { it.id }), cards)
        assertEquals(4, shown.cards.size)
        assertEquals(2, shown.decoyIds.size)
        assertTrue(shown.options.isEmpty())
        assertTrue(shown.cards.map { it.answer(Direction.REVERSE) }.let { it.distinct().size == it.size })
    }

    @Test
    fun otherModesShowJustTheirCards() = runBlocking {
        val cards = cards()
        val card = cards.values.first()
        val shown = StepPresenter(repo, Direction.FORWARD, Random(1)).present(SessionPlanner.Step(StudyMode.TYPE, listOf(card.id)), cards)
        assertEquals(listOf(card), shown.cards)
        assertTrue(shown.options.isEmpty() && shown.decoyIds.isEmpty())
    }
}
