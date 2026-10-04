package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ui.PAUSE_NOW_LABEL
import dev.denlogv.lexislearned.ui.detail
import dev.denlogv.lexislearned.ui.discardWarning
import dev.denlogv.lexislearned.ui.headline
import dev.denlogv.lexislearned.ui.pauseAfterSectionLabel
import dev.denlogv.lexislearned.ui.plural
import dev.denlogv.lexislearned.ui.resumeLabel
import dev.denlogv.lexislearned.ui.summary

/**
 * What the banner above the deck list shows about deck generation.
 *
 * @property title the headline.
 * @property detail the second line: counts, or the reason for a failure.
 * @property progress how far generation is, from 0 to 1, or null if there is no bar.
 * @property deckId the deck to open when the banner is tapped; null opens the EPUB screen instead, unless the banner is not [tappable].
 * @property pauseAfterSectionLabel the text of the button that pauses generation once the section in progress is done, or null if
 * generation is not running or that pause was asked for already.
 * @property pauseNowLabel the text of the button that pauses generation at once, or null if it is not running.
 * @property resumeLabel the text of the button that resumes a paused generation, or null if there is none to resume.
 * @property discardWarning what to tell the user before the rest of a paused generation is given up, or null if there is nothing to
 * discard: it also decides whether the banner offers the button.
 * @property tappable whether tapping the banner itself does anything; a paused generation has its buttons on the banner, and nowhere
 * to go to.
 */
data class BannerUi(
    val title: String,
    val detail: String,
    val progress: Float?,
    val deckId: Long?,
    val pauseAfterSectionLabel: String? = null,
    val pauseNowLabel: String? = null,
    val resumeLabel: String? = null,
    val discardWarning: String? = null,
    val tappable: Boolean = true,
) {
    /** Whether generation is running, which is when it can be paused. */
    val running: Boolean get() = pauseNowLabel != null

    /** Whether the user can dismiss the banner: a result can be, a generation that is running or can be resumed cannot. */
    val dismissible: Boolean get() = !running && resumeLabel == null
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
        pauseAfterSectionLabel = pauseAfterSectionLabel(),
        pauseNowLabel = PAUSE_NOW_LABEL,
    )
    // Everything that can be done with the rest is on the banner itself, so tapping the banner goes nowhere.
    is GenState.Paused -> BannerUi(
        title = headline(),
        detail = detail(),
        progress = null,
        deckId = null,
        resumeLabel = resumeLabel(),
        discardWarning = discardWarning(),
        tappable = false,
    )
    is GenState.Finished -> BannerUi("Deck ready", plural(cards, "card"), null, deckId)
    is GenState.Failed -> BannerUi("Deck generation failed", message, null, null)
    else -> null
}
