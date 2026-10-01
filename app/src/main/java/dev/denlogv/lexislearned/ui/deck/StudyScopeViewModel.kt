package dev.denlogv.lexislearned.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.StudyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Base of the book, part and chapter view models: they share the study options (direction and modes) and the number of
 * sessions that completes a word.
 *
 * @param settings the user's settings.
 */
abstract class StudyScopeViewModel(private val settings: Settings) : ViewModel() {
    /** The user's settings. */
    val prefs: StateFlow<Prefs> = settings.prefs

    /**
     * Changes which side of a card is the question.
     *
     * @param d the direction.
     */
    fun setDirection(d: Direction) = settings.setDirection(d)

    /**
     * Selects or deselects a study mode; at least one mode stays selected.
     *
     * @param m the mode.
     */
    fun toggleMode(m: StudyMode) = settings.toggleMode(m)

    /** The number of sessions that completes a word; emits again when the user changes it. */
    protected val needed: Flow<Int> get() = settings.prefs.map { it.sessionsToComplete }.distinctUntilChanged()

    /**
     * Keeps a flow running while the screen is visible, plus a short grace period for rotation.
     *
     * @receiver the flow.
     * @param initial the value before the first emission.
     * @return the flow as a state.
     */
    protected fun <T> Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

    private companion object {
        /** How long a flow stays active after the last screen stops watching it. */
        const val STOP_TIMEOUT_MS = 5000L
    }
}
