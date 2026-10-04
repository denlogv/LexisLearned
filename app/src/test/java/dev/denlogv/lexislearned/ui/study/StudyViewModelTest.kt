package dev.denlogv.lexislearned.ui.study

import androidx.lifecycle.SavedStateHandle
import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.data.testSettings
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.StudyMode
import dev.denlogv.lexislearned.sampleDeck
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudyViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val db = memoryDb()
    private val storage = DeckStorage(db)
    private val study = StudyRepository(db)
    private val settings: Settings = testSettings().also { it.setSessionSize(4) }

    private suspend fun start(chapterId: Long = -1): StudyViewModel {
        val deckId = storage.import(sampleDeck())
        val handle = SavedStateHandle(mapOf("deckId" to deckId, "chapterId" to chapterId, "partId" to -1L))
        return StudyViewModel(study, settings, handle, Random(3)).also { vm -> vm.ui.await { !it.loading && it.current.isNotEmpty() } }
    }

    /** Answers the current step the way a perfect learner would and waits until the next one is shown. */
    private suspend fun StudyViewModel.playStep() {
        val before = ui.value.stepsDone
        val current = ui.value
        when (current.mode) {
            StudyMode.LEARN -> if (current.exposure) introduced() else answer(Grade.GOOD)
            StudyMode.PAIR -> answerPair(emptySet())
            else -> answer(Grade.GOOD)
        }
        ui.await { it.stepsDone > before }
        ui.await { it.finished || it.current.isNotEmpty() }
    }

    @Test
    fun aPerfectSessionCompletesEveryWordAndSavesProgress() = runBlocking {
        val vm = start()
        assertEquals(4, vm.ui.value.totalWords)
        assertEquals(Direction.FORWARD, vm.direction)
        while (!vm.ui.value.finished) vm.playStep()
        val ui = vm.ui.value
        assertEquals(4, ui.completedWords)
        assertEquals(0, ui.wrong)
        assertTrue(ui.correct > 0)
        assertEquals(4, db.cardDao().cards(1).count { it.sessionsDone == 1 })
    }

    @Test
    fun wrongAnswersAreCountedAndRetried() = runBlocking {
        settings.setSessionSize(1)
        StudyMode.entries.filter { it != StudyMode.TYPE }.forEach(settings::toggleMode)
        val vm = start()
        assertEquals(StudyMode.TYPE, vm.ui.value.mode)
        val stepsBefore = vm.ui.value.stepsLeft
        vm.answer(Grade.AGAIN)
        vm.ui.await { it.wrong == 1 && it.stepsDone == 1 }
        assertTrue(vm.ui.value.stepsLeft >= stepsBefore)
        while (!vm.ui.value.finished) vm.playStep()
        assertEquals(1, vm.ui.value.completedWords)
    }

    @Test
    fun knownWordsFinishAtOnce() = runBlocking {
        settings.setSessionSize(1)
        val vm = start()
        assertTrue(vm.ui.value.exposure)
        vm.known()
        val ui = vm.ui.await { it.finished }
        assertEquals(1, ui.completedWords)
        assertEquals(1, db.cardDao().cards(1).count { it.sessionsDone == 1 })
    }

    @Test
    fun pairStepsGradeWordsThatWereMissed() = runBlocking {
        settings.setSessionSize(2)
        StudyMode.entries.filter { it != StudyMode.PAIR }.forEach(settings::toggleMode)
        val vm = start()
        assertEquals(StudyMode.PAIR, vm.ui.value.mode)
        assertTrue(vm.ui.value.current.size >= 4)
        val missed = vm.ui.value.current.first().id
        vm.answerPair(setOf(missed))
        vm.ui.await { it.wrong >= 1 }
        assertTrue(vm.ui.value.correct >= 1)
    }

    @Test
    fun emptyLibraryMeansNothingToStudy() = runBlocking {
        val handle = SavedStateHandle(mapOf("deckId" to 77L, "chapterId" to -1L, "partId" to -1L))
        val vm = StudyViewModel(study, settings, handle, Random(1))
        val ui = vm.ui.await { it.finished }
        assertEquals(0, ui.totalWords)
        vm.answer(Grade.GOOD)
        vm.introduced()
        vm.known()
        vm.answerPair(emptySet())
        assertEquals(0, vm.ui.value.correct)
    }

    @Test
    fun aChapterSessionOnlyUsesThatChapter() = runBlocking {
        val deckId = storage.import(sampleDeck())
        val chapter = db.structureDao().chapters(deckId)[1].id
        val handle = SavedStateHandle(mapOf("deckId" to deckId, "chapterId" to chapter, "partId" to -1L))
        val vm = StudyViewModel(study, settings, handle, Random(3))
        assertEquals(4, vm.ui.await { !it.loading }.totalWords)
    }
}
