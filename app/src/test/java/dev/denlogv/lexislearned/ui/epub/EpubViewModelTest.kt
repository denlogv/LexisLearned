package dev.denlogv.lexislearned.ui.epub

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ai.GenerationManager
import dev.denlogv.lexislearned.ai.LlmException
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
    private val generation = GenerationManager(CoroutineScope(Dispatchers.Unconfined), DeckStorage(memoryDb()), settings) { _, _ ->
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

    private fun partialManager(): GenerationManager {
        settings.setApiKey(Provider.ANTHROPIC, "k")
        return GenerationManager(CoroutineScope(Dispatchers.Unconfined), DeckStorage(memoryDb()), settings) { _, _ ->
            ScriptedLlm { _, user ->
                when {
                    user.startsWith("Give") -> "{}"
                    user.contains("Chapter Two") -> throw LlmException("busy", 500)
                    else -> """{"cards":[{"a":"alpha","b":"альфа"}]}"""
                }
            }
        }
    }

    /** Generates the test book with the second chapter failing, so the deck ends up with a section missing. */
    private suspend fun GenerationManager.runWithOneSectionMissing(): GenState.Finished {
        load(loremEpub())
        val book = (state.await { it is GenState.Ready } as GenState.Ready).book
        start(book, book.defaultSelection, "en", 8)
        return state.await { it is GenState.Finished } as GenState.Finished
    }

    @Test
    fun continuingReviewsTheMissingSectionsAndAddsToTheSameDeck() = runBlocking {
        val partial = partialManager()
        val screen = EpubViewModel(partial, settings, files) // the screen is open while the deck is generated
        val stopped = partial.runWithOneSectionMissing()
        screen.continueGeneration() // from the result panel
        assertEquals(setOf(3), screen.review.await { it.selected.isNotEmpty() }.selected)
        assertEquals(stopped.deckId, screen.target.value?.continuation?.deckId)
        screen.generate()
        val done = partial.state.await {
            it is GenState.Finished && it.deckId == stopped.deckId && it.cards >= stopped.cards
        } as GenState.Finished
        assertEquals(stopped.deckId, done.deckId)
        assertNull(screen.target.value)
    }

    @Test
    fun leavingTheReviewOfTheRestKeepsTheResultAndOpeningTheScreenAgainStartsFresh() = runBlocking {
        val partial = partialManager()
        val stopped = partial.runWithOneSectionMissing()
        val review = EpubViewModel(partial, settings, files, continuing = true)
        assertEquals(setOf(3), review.target.value?.continuation?.selection)
        assertEquals(stopped, partial.state.value) // backing out of the review changes nothing: the card and its Continue stay
        val fresh = EpubViewModel(partial, settings, files)
        assertEquals(GenState.Idle, fresh.state.value)
        assertNull(fresh.target.value) // no stale book: the user can choose another one
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
        vm.update { it.copy(selected = setOf(2), density = 3) }
        vm.setLevel(CefrLevel.C1)
        assertEquals(CefrLevel.C1, vm.prefs.value.level)
        vm.generate()
        val done = vm.state.await { it is GenState.Finished } as GenState.Finished
        assertEquals(1, done.cards)
    }

    @Test
    fun generateAndCancelDoNothingWithoutABook() {
        vm.generate()
        vm.cancel()
        assertEquals(GenState.Idle, vm.state.value)
    }
}
