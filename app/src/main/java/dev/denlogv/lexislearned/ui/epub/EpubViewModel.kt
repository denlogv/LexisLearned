package dev.denlogv.lexislearned.ui.epub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ai.GenerationManager
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.ui.library.DeckFiles
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The state and actions of the "deck from EPUB" screen.
 *
 * @param generation runs the generation and reports its stage.
 * @param settings the user's settings.
 * @param files reads the file the user picks.
 */
class EpubViewModel(private val generation: GenerationManager, private val settings: Settings, private val files: DeckFiles) : ViewModel() {
    private val _review = MutableStateFlow(ReviewState(emptySet(), "en"))

    /** The current stage of reading and generating. */
    val state: StateFlow<GenState> = generation.state

    /** The current settings. */
    val prefs: StateFlow<Prefs> = settings.prefs

    /** The choices made while reviewing the book. */
    val review: StateFlow<ReviewState> = _review

    init {
        generation.reset()
        viewModelScope.launch {
            generation.state.filterIsInstance<GenState.Ready>().map { it.book }.distinctUntilChanged()
                .collect { _review.value = ReviewState.initial(it) }
        }
    }

    /**
     * Reads the chosen EPUB and moves on to the review.
     *
     * @param uri the file's address as given by the file picker.
     */
    fun choose(uri: String) {
        viewModelScope.launch {
            try {
                generation.load(files.read(uri).bytes)
            } catch (e: IOException) {
                generation.fail(e.message ?: "Could not read the file")
            }
        }
    }

    /**
     * Changes the review choices.
     *
     * @param change computes the new choices from the current ones.
     */
    fun update(change: (ReviewState) -> ReviewState) {
        _review.value = change(_review.value)
    }

    /**
     * Sets the level used for the cards.
     *
     * @param level the level.
     */
    fun setLevel(level: CefrLevel) = settings.setLevel(level)

    /** Starts generating the deck from the reviewed book, if there is one. */
    fun generate() {
        val book = (state.value as? GenState.Ready)?.book ?: return
        val choices = review.value
        generation.start(book, choices.selected, choices.lang, choices.density)
    }

    /** Stops after the current section and keeps what is finished. */
    fun cancel() = generation.cancel()

    /** Goes back to choosing a file. */
    fun restart() = generation.reset()
}
