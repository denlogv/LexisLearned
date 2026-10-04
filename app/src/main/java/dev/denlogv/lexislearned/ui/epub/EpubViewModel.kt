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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The state and actions of the "deck from EPUB" screen.
 *
 * @param generation runs the generation and reports its stage.
 * @param settings the user's settings.
 * @param files reads the file the user picks.
 * @param continuing whether the screen was opened to add the rest of a book to a deck that was stopped early. Otherwise it starts
 * afresh, and nothing left over from an earlier book is shown.
 */
class EpubViewModel(
    private val generation: GenerationManager,
    private val settings: Settings,
    private val files: DeckFiles,
    continuing: Boolean = false,
) : ViewModel() {
    private val _review = MutableStateFlow(ReviewState(emptySet(), "en"))
    private val continuingRest = MutableStateFlow(continuing)

    /** The current stage of reading and generating. */
    val state: StateFlow<GenState> = generation.state

    /** The current settings. */
    val prefs: StateFlow<Prefs> = settings.prefs

    /** The choices made while reviewing the book. */
    val review: StateFlow<ReviewState> = _review

    /** The book to review, or null when the screen shows something else: choosing a file, progress or a result. */
    val target: StateFlow<ReviewTarget?>

    init {
        if (!continuing) generation.reset()
        target = combine(generation.state, continuingRest, ::reviewTargetOf)
            .stateIn(viewModelScope, SharingStarted.Eagerly, reviewTargetOf(generation.state.value, continuing))
        viewModelScope.launch {
            target.filterNotNull().distinctUntilChanged().collect { _review.value = ReviewState.initial(it.book, it.continuation) }
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

    /** Starts generating the deck from the reviewed book, if there is one; for a deck that was stopped early, adds to that deck. */
    fun generate() {
        val reviewed = target.value ?: return
        val choices = review.value
        generation.start(reviewed.book, choices.selected, choices.lang, choices.density, reviewed.continuation)
        continuingRest.value = false
    }

    /** Stops after the current section and keeps what is finished. */
    fun cancel() = generation.cancel()

    /** Shows the review of the book to add the rest to the deck that was stopped early; the deck's result stays as it is until then. */
    fun continueGeneration() {
        continuingRest.value = true
    }

    /** Goes back to choosing a file. */
    fun restart() = generation.reset()
}
