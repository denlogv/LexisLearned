package dev.denlogv.lexislearned.ui.study

import androidx.lifecycle.SavedStateHandle
import dev.denlogv.lexislearned.InlineDispatcher
import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.DeckLibrary
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.MemorySessionStore
import dev.denlogv.lexislearned.data.SessionKeeper
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.data.testSettings
import dev.denlogv.lexislearned.sampleDeck
import kotlin.random.Random
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The pieces the tests of leaving a session and coming back to it share: a deck, a store of saved sessions and a clock. */
@RunWith(RobolectricTestRunner::class)
abstract class StudyResumeFixture {
    @get:Rule val main = MainDispatcherRule()

    protected val db = memoryDb()
    protected val storage = DeckStorage(db)
    protected val study = StudyRepository(db)
    protected val library = DeckLibrary(db)
    protected val settings: Settings = testSettings().also { it.setSessionSize(4) }
    protected val store = MemorySessionStore()
    protected var now = 1_000_000L
    protected val keeper = SessionKeeper(store, { now }, InlineDispatcher)
    protected var deckId = 0L

    @After
    fun closeDatabase() = db.close()

    protected suspend fun importDeck() {
        deckId = storage.import(sampleDeck())
    }

    protected suspend fun chapter(index: Int): Long = db.structureDao().chapters(deckId)[index].id

    /** Opens the screen of a scope, as if its Study button was pressed, and waits until it shows something. */
    protected suspend fun open(chapterId: Long = -1, partId: Long = -1, seed: Int = 3): StudyViewModel {
        val handle = SavedStateHandle(mapOf("deckId" to deckId, "chapterId" to chapterId, "partId" to partId))
        return StudyViewModel(study, settings, handle, keeper, Random(seed)).also { vm -> vm.ui.await { !it.loading } }
    }

    /** Plays a few steps, the first graded one of them wrong, and leaves the screen without finishing. */
    protected suspend fun StudyViewModel.playAndLeave(steps: Int = 4) {
        var missed = false
        repeat(steps) {
            val current = ui.value
            val graded = !current.exposure
            answerLike(current, correct = missed || !graded)
            missed = missed || graded
            awaitNextStep(current)
        }
    }
}
