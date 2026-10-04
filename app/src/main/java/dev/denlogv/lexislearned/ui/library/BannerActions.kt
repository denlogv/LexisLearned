package dev.denlogv.lexislearned.ui.library

/**
 * What the buttons and taps of the generation banner do.
 *
 * @property onOpenDeck opens the finished deck with the id given and dismisses the banner.
 * @property onPauseAfterSection pauses a running generation once the section in progress is done.
 * @property onPauseNow pauses a running generation at once, giving up the request in flight.
 * @property onResume resumes a paused generation.
 * @property onChooseSections opens the review to pick the sections of a paused generation.
 * @property onDiscard gives up on the sections that are left of a paused generation; asked for only after the user confirmed.
 * @property onDismiss dismisses a result.
 */
class BannerActions(
    val onOpenDeck: (Long) -> Unit,
    val onPauseAfterSection: () -> Unit,
    val onPauseNow: () -> Unit,
    val onResume: () -> Unit,
    val onChooseSections: () -> Unit,
    val onDiscard: () -> Unit,
    val onDismiss: () -> Unit,
)
