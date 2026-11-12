package dev.denlogv.lexislearned.data

import android.content.SharedPreferences

/**
 * Whether study sessions are whole chapters, kept apart from three facts that are easy to mix up: what the learner chose
 * explicitly, what the default was when the settings were first opened, and whether the one-time suggestion was seen.
 *
 * The default is on, except for a learner who had already chosen a number of cards or of new cards by then: their numbers
 * stay in force, so for them it starts off. The default is stored once and never recomputed, because the numbers are saved
 * the moment they change and a later change must not flip the switch. An explicit choice always wins over the default. Only
 * a learner whose switch is off for that reason alone, and who has not decided since, is offered the suggestion to switch it on.
 *
 * @param sp where the facts are stored.
 * @param numbersStored whether a number of cards or of new cards was already stored when the settings were opened.
 */
internal class WholeChapterChoice(private val sp: SharedPreferences, numbersStored: Boolean) {
    init {
        if (!sp.contains(DEFAULT_ON)) sp.edit().putBoolean(DEFAULT_ON, !numbersStored).apply()
    }

    /** Whether sessions are whole chapters: the explicit choice, or the default when none was made. */
    val isOn: Boolean get() = if (sp.contains(CHOICE)) sp.getBoolean(CHOICE, true) else defaultOn

    /**
     * Whether to offer the one-time suggestion to use whole chapters: the switch is off only because the learner's numbers
     * predate it, nothing was chosen on the switch since, and the suggestion was not answered or dismissed yet.
     */
    val shouldSuggest: Boolean get() = !sp.contains(CHOICE) && !defaultOn && !sp.getBoolean(SUGGESTION_SEEN, false)

    private val defaultOn: Boolean get() = sp.getBoolean(DEFAULT_ON, true)

    /**
     * Records the learner's own choice, which from now on beats the default.
     *
     * @param on true for whole chapters.
     */
    fun choose(on: Boolean) = sp.edit().putBoolean(CHOICE, on).apply()

    /** Records that the suggestion was answered or dismissed, so it is never shown again. */
    fun markSuggestionSeen() = sp.edit().putBoolean(SUGGESTION_SEEN, true).apply()

    private companion object {
        const val CHOICE = "wholeChapterChoice"
        const val DEFAULT_ON = "wholeChapterDefault"
        const val SUGGESTION_SEEN = "wholeChapterSuggestionSeen"
    }
}
