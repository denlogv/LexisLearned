package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ai.GenerationManager
import dev.denlogv.lexislearned.ai.MemoryJobStore
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.DeckLibrary
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.memoryDb
import dev.denlogv.lexislearned.data.testSettings
import dev.denlogv.lexislearned.format.NativeFormat
import dev.denlogv.lexislearned.sampleDeck
import dev.denlogv.lexislearned.ui.MemoryFiles
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val files = MemoryFiles()
    private val db = memoryDb()
    private val library = DeckLibrary(db)
    private val storage = DeckStorage(db)

    // Created lazily: a view model must be built after the rule has replaced the main dispatcher.
    private val settings = testSettings()
    private val generation = GenerationManager(CoroutineScope(Dispatchers.Unconfined), storage, settings, MemoryJobStore())
    private val vm by lazy { LibraryViewModel(library, storage, settings, files, generation) }

    private suspend fun next(): String = withTimeout(10_000) { vm.messages.first() }

    private fun lexisBytes() = ByteArrayOutputStream().also { NativeFormat.write(sampleDeck(), it) }.toByteArray()

    @Test
    fun importShowsTheDeckInTheLibrary() = runBlocking {
        files.files["deck.lexis"] = lexisBytes()
        vm.import("deck.lexis")
        assertEquals("Imported “Lorem” (12 cards)", next())
        val decks = vm.decks.await { !it.isNullOrEmpty() }!!
        assertEquals(12, decks.single().total)
    }

    @Test
    fun importReportsUnreadableAndMissingFiles() = runBlocking {
        files.files["bad.lexis"] = "not json".toByteArray()
        vm.import("bad.lexis")
        assertTrue(next().startsWith("Import failed"))
        vm.import("missing.lexis")
        assertTrue(next().startsWith("Import failed: missing"))
    }

    @Test
    fun exportWritesTheDeck() = runBlocking {
        val id = storage.import(sampleDeck())
        vm.export("out.lexis", id, NativeFormat)
        assertEquals("Exported 12 cards", next())
        assertTrue(String(files.files.getValue("out.lexis")).contains("Lorem"))
    }

    @Test
    fun resetAndDeleteChangeTheLibrary() = runBlocking {
        val id = storage.import(sampleDeck())
        vm.reset(id)
        assertEquals("Progress reset", next())
        vm.decks.await { !it.isNullOrEmpty() }
        vm.delete(id)
        vm.decks.await { it != null && it.isEmpty() }
        Unit
    }

    @Test
    fun generationProgressIsExposedAndCanBeDismissed() = runBlocking {
        generation.fail("offline")
        assertEquals(GenState.Failed("offline"), vm.generating.value)
        vm.pauseGeneration() // nothing is running or paused, so these change nothing
        vm.resumeGeneration()
        vm.dismissGeneration()
        assertEquals(GenState.Idle, vm.generating.value)
    }
}
