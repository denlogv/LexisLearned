package dev.denlogv.lexislearned.ui.epub

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ai.GenerationManager
import dev.denlogv.lexislearned.ai.LlmException
import dev.denlogv.lexislearned.ai.MemoryJobStore
import dev.denlogv.lexislearned.ai.ScriptedLlm
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.Provider
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.data.testSettings
import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.loremEpub
import dev.denlogv.lexislearned.ui.MemoryFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpubViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val settings = testSettings()
    private val files = MemoryFiles()
    private val generation =
        GenerationManager(CoroutineScope(Dispatchers.Unconfined), DeckStorage(memoryDb()), settings, MemoryJobStore()) { _, _ ->
            ScriptedLlm { _, user ->
                if (user.startsWith("Give")) "{}" else """{"cards":[{"a":"alpha","b":"альфа"}]}"""
            }
        }

    // Created lazily: a view model must be built after the rule has replaced the main dispatcher.
    private val vm by lazy { EpubViewModel(generation, settings, files) }

    @Test
    fun choosingABookStartsTheReviewWithSuggestedSections() = runBlocking {
        files.files["book.epub"] = loremEpub()
        vm.choose("book.epub")
        vm.state.await { it is GenState.Ready }
        val review = vm.review.await { it.selected.isNotEmpty() }
        assertEquals(setOf(2, 3), review.selected)
        assertEquals("en", review.lang)
    }

    private var chapterTwoFails = true

    private fun partialManager(): GenerationManager {
        settings.setApiKey(Provider.ANTHROPIC, "k")
        return GenerationManager(CoroutineScope(Dispatchers.Unconfined), DeckStorage(memoryDb()), settings, MemoryJobStore()) { _, _ ->
            ScriptedLlm { _, user ->
                when {
                    user.startsWith("Give") -> "{}"
                    user.contains("Chapter Two") && chapterTwoFails -> throw LlmException("busy", 500)
                    else -> """{"cards":[{"a":"alpha","b":"альфа"}]}"""
                }
            }
        }
    }

    /** Generates the test book with the second chapter failing, so the deck ends up with a section missing. */
    private suspend fun GenerationManager.runWithOneSectionMissing(): GenState.Paused {
        load(loremEpub())
        val book = (state.await { it is GenState.Ready } as GenState.Ready).book
        start(book, book.defaultSelection, "en")
        return state.await { it is GenState.Paused } as GenState.Paused
    }

    @Test
    fun choosingTheSectionsReviewsTheMissingOnesAndAddsToTheSameDeck() = runBlocking {
        val partial = partialManager()
        val paused = partial.runWithOneSectionMissing()
        val screen = EpubViewModel(partial, settings, files, continuing = true) // opened by "Choose sections" on the banner
        assertEquals(setOf(3), screen.review.await { it.selected == setOf(3) }.selected) // set on another thread, so wait for it
        assertEquals(paused.deckId, screen.target.value?.continuation?.deckId)
        chapterTwoFails = false
        screen.generate()
        val done = partial.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(paused.deckId, done.deckId)
        assertNull(screen.target.value)
    }

    @Test
    fun leavingTheReviewKeepsThePauseAndOpeningTheScreenAgainStillShowsIt() = runBlocking {
        val partial = partialManager()
        val paused = partial.runWithOneSectionMissing()
        val review = EpubViewModel(partial, settings, files, continuing = true)
        assertEquals(setOf(3), review.target.value?.continuation?.selection)
        assertEquals(paused, partial.state.value) // backing out of the review changes nothing: the pause and its buttons stay
        val again = EpubViewModel(partial, settings, files)
        assertEquals(paused, again.state.value) // only the user ends a pause, so opening the screen does not
        assertNull(again.target.value)
    }

    @Test
    fun aReviewedBookCanBeReplacedByChoosingAnother() = runBlocking {
        files.files["book.epub"] = loremEpub()
        vm.choose("book.epub")
        vm.state.await { it is GenState.Ready }
        vm.restart()
        assertEquals(GenState.Idle, vm.state.value)
        assertNull(vm.target.value)
    }

    @Test
    fun unreadableFilesFail() = runBlocking {
        vm.choose("missing.epub")
        val failed = vm.state.await { it is GenState.Failed } as GenState.Failed
        assertTrue(failed.message.contains("missing"))
        vm.restart()
        assertEquals(GenState.Idle, vm.state.value)
    }

    @Test
    fun reviewChoicesGoIntoGeneration() = runBlocking {
        settings.setApiKey(Provider.ANTHROPIC, "sk-test")
        files.files["book.epub"] = loremEpub()
        vm.choose("book.epub")
        vm.state.await { it is GenState.Ready }
        vm.update { it.copy(selected = setOf(2)) }
        vm.setLevel(CefrLevel.C1)
        assertEquals(CefrLevel.C1, vm.prefs.value.level)
        vm.generate()
        val done = vm.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(1, done.cards)
    }

    @Test
    fun generateDoesNothingWithoutABook() {
        vm.generate()
        assertEquals(GenState.Idle, vm.state.value)
    }
}
