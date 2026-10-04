package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ui.headline
import dev.denlogv.lexislearned.ui.pauseLabel
import dev.denlogv.lexislearned.ui.plural
import dev.denlogv.lexislearned.ui.resumeLabel
import dev.denlogv.lexislearned.ui.summary

/**
 * What the banner above the deck list shows about deck generation.
 *
 * @property title the headline.
 * @property detail the second line: counts, or the reason for a failure.
 * @property progress how far generation is, from 0 to 1, or null if there is no bar.
 * @property deckId the deck to open when the banner is tapped; null opens the EPUB screen instead.
 * @property pauseLabel the text of the button that pauses generation, or null if it is not running.
 * @property resumeLabel the text of the button that resumes a paused generation, or null if there is none to resume.
 */
data class BannerUi(
    val title: String,
    val detail: String,
    val progress: Float?,
    val deckId: Long?,
    val pauseLabel: String? = null,
    val resumeLabel: String? = null,
) {
    /** Whether the user can dismiss the banner: a result can be, a generation that is running or can be resumed cannot. */
    val dismissible: Boolean get() = pauseLabel == null && resumeLabel == null
}

/**
 * Works out the banner for a generation state.
 *
 * @receiver the state of the "deck from EPUB" job.
 * @return the banner, or null when the job is idle or still being set up and there is nothing to report in the library.
 */
fun GenState.toBanner(): BannerUi? = when (this) {
    is GenState.Running -> BannerUi(
        title = "Generating “$title”",
        detail = summary(),
        progress = if (total == 0) 0f else done / total.toFloat(),
        deckId = null,
        pauseLabel = pauseLabel(),
    )
    // Not the deck: tapping opens the generation screen, where the deck so far, the sections and discarding are offered.
    is GenState.Paused -> BannerUi(headline(), summary(), null, null, resumeLabel = resumeLabel())
    is GenState.Finished -> BannerUi("Deck ready", plural(cards, "card"), null, deckId)
    is GenState.Failed -> BannerUi("Deck generation failed", message, null, null)
    else -> null
}
