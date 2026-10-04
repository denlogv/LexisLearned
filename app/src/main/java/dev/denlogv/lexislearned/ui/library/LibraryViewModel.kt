package dev.denlogv.lexislearned.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ai.GenerationManager
import dev.denlogv.lexislearned.data.DeckLibrary
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.format.DeckFormat
import dev.denlogv.lexislearned.format.FormatException
import dev.denlogv.lexislearned.format.FormatRegistry
import dev.denlogv.lexislearned.ui.plural
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The state and actions of the library screen: the list of decks, importing, exporting, resetting and deleting.
 *
 * @param library the stored decks.
 * @param storage imports and exports whole decks.
 * @param settings the user's settings; the number of sessions that completes a word affects the progress shown.
 * @param files reads and writes the files the user picks.
 * @param generation the "deck from EPUB" job, whose progress is shown above the list.
 */
class LibraryViewModel(
    private val library: DeckLibrary,
    private val storage: DeckStorage,
    settings: Settings,
    private val files: DeckFiles,
    private val generation: GenerationManager,
) : ViewModel() {
    private val outbox = Channel<String>(Channel.BUFFERED)

    /** All decks with their progress; null until the first load. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val decks: StateFlow<List<dev.denlogv.lexislearned.data.DeckSummary>?> = settings.prefs.map { it.sessionsToComplete }
        .distinctUntilChanged()
        .flatMapLatest { library.decks(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** The state of deck generation, shown as a banner so it stays visible outside the EPUB screen. */
    val generating: StateFlow<GenState> = generation.state

    /** One-off messages for the user, such as the result of an import. */
    val messages: Flow<String> = outbox.receiveAsFlow()

    /** Pauses deck generation after the current section; chapters that are finished stay in the library. */
    fun pauseGeneration() = generation.pause()

    /** Resumes a paused generation with the sections that are left. */
    fun resumeGeneration() = generation.resume()

    /** Dismisses the result of a finished or failed generation. */
    fun dismissGeneration() = generation.reset()

    /**
     * Imports the chosen file as a deck and reports the result.
     *
     * @param uri the file's address as given by the file picker.
     * @return the running job.
     */
    fun import(uri: String): Job = viewModelScope.launch {
        outbox.send(
            try {
                val file = files.read(uri)
                val deck = withContext(Dispatchers.Default) { FormatRegistry.read(file.name, file.bytes) }
                storage.import(deck)
                "Imported “${deck.title}” (${plural(deck.cardCount, "card")})"
            } catch (e: FormatException) {
                "Import failed: ${e.message}"
            } catch (e: IOException) {
                "Import failed: ${e.message}"
            },
        )
    }

    /**
     * Exports a deck to the chosen file and reports the result.
     *
     * @param uri where to write, as given by the file picker.
     * @param deckId the deck to export.
     * @param format the file format to write.
     * @return the running job.
     */
    fun export(uri: String, deckId: Long, format: DeckFormat): Job = viewModelScope.launch {
        outbox.send(
            try {
                val deck = storage.export(deckId)
                files.write(uri) { format.write(deck, it) }
                "Exported ${plural(deck.cardCount, "card")}"
            } catch (e: FormatException) {
                "Export failed: ${e.message}"
            } catch (e: IOException) {
                "Export failed: ${e.message}"
            },
        )
    }

    /**
     * Makes every word of a deck new again and reports it.
     *
     * @param id the deck.
     * @return the running job.
     */
    fun reset(id: Long): Job = viewModelScope.launch {
        library.resetProgress(id)
        outbox.send("Progress reset")
    }

    /**
     * Deletes a deck.
     *
     * @param id the deck.
     * @return the running job.
     */
    fun delete(id: Long): Job = viewModelScope.launch { library.delete(id) }

    private companion object {
        /** How long the deck list stays active after the last screen stops watching it. */
        const val STOP_TIMEOUT_MS = 5000L
    }
}
