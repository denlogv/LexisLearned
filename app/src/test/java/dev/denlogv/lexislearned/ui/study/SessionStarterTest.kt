package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.InlineDispatcher
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.MemorySessionStore
import dev.denlogv.lexislearned.data.SessionKeeper
import dev.denlogv.lexislearned.data.SessionProgress
import dev.denlogv.lexislearned.data.SessionScope
import dev.denlogv.lexislearned.data.SessionSettings
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.data.testSettings
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.sampleDeck
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionStarterTest {
    private val db = memoryDb()
    private val store = MemorySessionStore()
    private val keeper = SessionKeeper(store, { 5_000 }, InlineDispatcher)
    private val study = StudyRepository(db)
    private val starter = SessionStarter(study, keeper, Random(1))
    private val settings = testSettings()

    @After
    fun close() = db.close()

    @Test
    fun aFreshSessionPlansTheWordsTheRepositoryChooses() = runBlocking {
        val deckId = DeckStorage(db).import(sampleDeck())
        settings.setWholeChapter(false)
        settings.setSessionSize(3)
        val start = starter.fresh(SessionScope(deckId), settings.prefs.value)
        assertEquals(3, start.cards.size)
        assertEquals(3, start.planner.activeCards)
        assertEquals(3, start.progress.totalWords)
        assertEquals(0, start.progress.stepsDone)
    }

    @Test
    fun aFreshSessionRunsWithTheCurrentSettings() = runBlocking {
        val deckId = DeckStorage(db).import(sampleDeck())
        settings.setDirection(Direction.REVERSE)
        val start = starter.fresh(SessionScope(deckId), settings.prefs.value)
        assertEquals(SessionSettings.of(settings.prefs.value), start.settings)
        assertEquals(settings.prefs.value.modes, start.planner.modes)
    }

    @Test
    fun aChapterSessionOnlyHasThatChaptersWords() = runBlocking {
        val deckId = DeckStorage(db).import(sampleDeck())
        val chapter = db.structureDao().chapters(deckId)[1].id
        val start = starter.fresh(SessionScope(deckId, chapterId = chapter), settings.prefs.value)
        assertTrue(start.cards.isNotEmpty())
        assertTrue(start.cards.all { it.chapterId == chapter })
    }

    @Test
    fun aDeckWithoutWordsGivesAnEmptySession() = runBlocking {
        val start = starter.fresh(SessionScope(77), settings.prefs.value)
        assertTrue(start.cards.isEmpty())
        assertEquals(0, start.planner.activeCards)
        assertEquals(0, start.progress.totalWords)
    }

    @Test
    fun nothingIsUnfinishedBeforeASessionWasKept() = runBlocking {
        assertNull(starter.unfinished(SessionScope(1)))
    }

    @Test
    fun aKeptSessionIsRestoredWithItsCountersAndWords() = runBlocking {
        val deckId = DeckStorage(db).import(sampleDeck())
        val scope = SessionScope(deckId)
        val start = starter.fresh(scope, settings.prefs.value)
        val step = start.planner.next()!!
        start.planner.answered(step, step.cardIds.associateWith { false })
        val progress = SessionProgress(1, 0, 1, 0, 4)
        keeper.keep(scope, start.settings, progress, start.planner.snapshot())
        val restored = starter.unfinished(scope)!!
        assertEquals(progress, restored.progress)
        assertEquals(start.settings, restored.settings)
        assertEquals(start.planner.stepsLeft, restored.planner.stepsLeft)
        assertEquals(start.cards.map { it.id }, restored.cards.map { it.id })
    }

    @Test
    fun theWordCountLosesTheWordsThatAreGone() = runBlocking {
        val deckId = DeckStorage(db).import(sampleDeck())
        val scope = SessionScope(deckId)
        val start = starter.fresh(scope, settings.prefs.value)
        keeper.keep(scope, start.settings, SessionProgress(0, 0, 0, 1, start.cards.size), start.planner.snapshot())
        val gone = start.cards.first().id
        db.openHelper.writableDatabase.execSQL("DELETE FROM cards WHERE id = $gone")
        val restored = starter.unfinished(scope)!!
        assertEquals(start.cards.size - 1, restored.progress.totalWords)
        assertEquals(1, restored.progress.completedWords)
        assertEquals(start.cards.size - 1, restored.cards.size)
    }

    @Test
    fun aSessionWithoutAnyWordLeftIsNotUnfinished() = runBlocking {
        val deckId = DeckStorage(db).import(sampleDeck())
        val scope = SessionScope(deckId)
        val start = starter.fresh(scope, settings.prefs.value)
        keeper.keep(scope, start.settings, SessionProgress(0, 0, 0, 0, 4), start.planner.snapshot())
        db.openHelper.writableDatabase.execSQL("DELETE FROM cards")
        assertNull(starter.unfinished(scope))
    }
}
