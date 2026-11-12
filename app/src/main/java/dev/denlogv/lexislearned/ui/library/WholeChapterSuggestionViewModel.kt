package dev.denlogv.lexislearned.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.denlogv.lexislearned.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The one-time offer to use whole-chapter sessions, made to a learner whose switch is off only because they had chosen a
 * number of cards before the switch existed.
 *
 * Whether to show it is decided by the settings ([dev.denlogv.lexislearned.data.Prefs.suggestWholeChapter]), which keeps the
 * "answered" mark in storage, so it survives a restart of the app and never comes back once answered or dismissed.
 *
 * @param settings the user's settings; the answer is saved there.
 */
class WholeChapterSuggestionViewModel(private val settings: Settings) : ViewModel() {
    /** Whether the offer is on screen: true until the learner answers or dismisses it. */
    val visible: StateFlow<Boolean> = settings.prefs.map { it.suggestWholeChapter }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.prefs.value.suggestWholeChapter)

    /** "Use whole chapters": switches the option on, which closes the offer for good. */
    fun useWholeChapters() {
        settings.markWholeChapterSuggestionSeen()
        settings.setWholeChapter(true)
    }

    /** "Keep my numbers", or the offer was dismissed: the option stays off and the offer does not come back. */
    fun keepNumbers() = settings.markWholeChapterSuggestionSeen()
}
