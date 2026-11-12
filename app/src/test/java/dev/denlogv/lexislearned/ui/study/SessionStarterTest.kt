package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.data.DeckStorage
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionStarterTest {
    private val db = memoryDb()
    private val starter = SessionStarter(StudyRepository(db), Random(1))
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
}
