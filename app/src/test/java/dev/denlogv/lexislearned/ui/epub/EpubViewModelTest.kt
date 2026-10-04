package dev.denlogv.lexislearned.ui.epub

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ai.GenerationManager
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
