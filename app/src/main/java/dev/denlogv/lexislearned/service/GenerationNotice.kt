package dev.denlogv.lexislearned.service

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ui.library.toBanner
import kotlin.math.roundToInt

/**
 * What the notification about deck generation says: the same words as the banner in the library, so the two never disagree.
 *
 * @property title the headline.
 * @property text the second line.
 * @property progressPercent how far generation is, from 0 to 100, or null if there is no bar.
 * @property pauseLabel the text of the button that pauses generation, or null if there is none: the notification is then a result.
 */
data class GenerationNotice(val title: String, val text: String, val progressPercent: Int?, val pauseLabel: String?) {
    /** Whether the notification belongs to a generation that is still going, so the user cannot swipe it away. */
    val ongoing: Boolean get() = pauseLabel != null

    /** Where the notices come from. */
    companion object {
        /** What the notification says before the first progress arrives. */
        val STARTING = GenerationNotice("Generating deck", "Starting…", 0, "Pause")

        private const val PERCENT = 100

        /**
         * Works out the notice for a generation state.
         *
         * @param state the state.
         * @return the notice, or null for a state that has nothing to tell, such as the review of a book.
         */
        fun of(state: GenState): GenerationNotice? = state.toBanner()?.let {
            GenerationNotice(it.title, it.detail, it.progress?.let { share -> (share * PERCENT).roundToInt() }, it.pauseLabel)
        }
    }
}
