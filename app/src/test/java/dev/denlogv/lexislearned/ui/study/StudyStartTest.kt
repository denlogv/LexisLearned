package dev.denlogv.lexislearned.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.data.AppDatabase
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.data.testContext
import dev.denlogv.lexislearned.data.testSettings
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.StudyMode
import dev.denlogv.lexislearned.sampleDeck
import java.util.concurrent.Executor
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Looks at what the study screen would be asked to draw while the first step of a session is still being prepared. */
@RunWith(RobolectricTestRunner::class)
class StudyStartTest {
    @get:Rule val main = MainDispatcherRule()

    /** Runs database work at once, or keeps it until the test lets it go, so the test can look at the state in between. */
    private class GatedExecutor : Executor {
        val held = ArrayDeque<Runnable>()
        var gated = false

        override fun execute(command: Runnable) {
            if (gated) held.addLast(command) else command.run()
        }
    }

    @Test
    fun theScreenIsNotAskedForAStepBeforeTheFirstOneIsReady() = runBlocking {
        val gate = GatedExecutor()
        val db = Room.inMemoryDatabaseBuilder(testContext(), AppDatabase::class.java).allowMainThreadQueries()
            .setQueryExecutor(gate).setTransactionExecutor(gate).build()
        val study = StudyRepository(db)
        val deckId = DeckStorage(db).import(sampleDeck())
        db.cardDao().cards(deckId).takeLast(6).forEach { study.finishSession(it, Grade.GOOD, spaced = true) }
        val settings = testSettings().also { it.setSessionSize(2) }
        StudyMode.entries.filter { it != StudyMode.PAIR }.forEach(settings::toggleMode)
        val handle = SavedStateHandle(mapOf("deckId" to deckId, "chapterId" to -1L, "partId" to -1L))
        gate.gated = true
        val vm = StudyViewModel(study, settings, handle, Random(3))
        // A Pair step needs filler words from the database, so its first step is ready only after several queries.
        val shown = mutableListOf(vm.ui.value)
        while (gate.held.isNotEmpty()) {
            gate.held.removeFirst().run()
            shown += vm.ui.value
        }
        assertTrue("the session never got its first step", vm.ui.value.current.isNotEmpty())
        val bare = shown.count { !it.loading && !it.finished && it.current.isEmpty() }
        assertTrue("the screen was asked to show a step without cards in $bare of ${shown.size} states", bare == 0)
    }
}
