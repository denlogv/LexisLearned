package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ui.plural

/**
 * What the banner above the deck list shows about deck generation.
 *
 * @property title the headline.
 * @property detail the second line: counts, or the reason for a failure.
 * @property progress how far generation is, from 0 to 1, or null if there is no bar.
 * @property deckId the deck to open when the banner is tapped; null opens the EPUB screen instead.
 * @property running whether generation is still going, which is when the banner offers to stop it.
 */
data class BannerUi(val title: String, val detail: String, val progress: Float?, val deckId: Long?, val running: Boolean)

/**
 * Works out the banner for a generation state.
 *
 * @receiver the state of the "deck from EPUB" job.
 * @return the banner, or null when the job is idle or still being set up and there is nothing to report in the library.
 */
fun GenState.toBanner(): BannerUi? = when (this) {
    is GenState.Running -> BannerUi(
        title = "Generating “$title”",
        detail = "$done / ${plural(total, "section")} · ${plural(cards, "card")} so far",
        progress = if (total == 0) 0f else done / total.toFloat(),
        deckId = null,
        running = true,
    )
    is GenState.Finished -> BannerUi(
        title = "Deck ready",
        detail = plural(cards, "card") + if (failed.isEmpty()) "" else " · ${failed.size} section(s) skipped",
        progress = null,
        deckId = deckId,
        running = false,
    )
    is GenState.Failed -> BannerUi("Deck generation failed", message, null, null, false)
    else -> null
}
