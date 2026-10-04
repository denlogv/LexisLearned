package dev.denlogv.lexislearned.ui

import dev.denlogv.lexislearned.ai.GenState

/**
 * The progress line of a running generation, for example "3 / 12 sections · 40 cards so far".
 *
 * @receiver the running state.
 * @return the line; it also says when the generation will pause after the current section.
 */
fun GenState.Running.summary(): String =
    "$done / ${plural(total, "section")} · ${plural(cards, "card")} so far" + if (pausing) " · pausing after this section" else ""

/**
 * The text of the button that pauses a running generation once the section being worked on is done, whose request is paid for
 * already.
 *
 * @receiver the running state.
 * @return the label, or null once that pause was asked for: there is nothing left to ask.
 */
fun GenState.Running.pauseAfterSectionLabel(): String? = if (pausing) null else PAUSE_AFTER_SECTION_LABEL

/** The text of the button that pauses a running generation once the section being worked on is done. */
const val PAUSE_AFTER_SECTION_LABEL = "Pause after section"

/** The text of the button that pauses a running generation at once, giving up the request in flight. */
const val PAUSE_NOW_LABEL = "Pause now"

/**
 * The headline of a generation that is not finished.
 *
 * @receiver the paused state.
 * @return "Generation paused", or a headline about the problem if there was one.
 */
fun GenState.Paused.headline(): String = if (failed.isEmpty()) "Generation paused" else "Generation stopped by a problem"

/**
 * The line under the headline of a generation that is not finished, for example "12 cards · 3 sections left · 1 failed".
 *
 * @receiver the paused state.
 * @return the line.
 */
fun GenState.Paused.summary(): String = listOfNotNull(
    plural(cards, "card"),
    "${plural(remaining.size, "section")} left",
    "${failed.size} failed".takeIf { failed.isNotEmpty() },
).joinToString(" · ")

/**
 * The text of the button that does the sections that are left.
 *
 * @receiver the paused state.
 * @return "Resume" after a plain pause, "Retry" after a problem.
 */
fun GenState.Paused.resumeLabel(): String = if (failed.isEmpty()) "Resume" else "Retry"

/**
 * The warning shown before the rest of a paused generation is given up: what is lost and what stays.
 *
 * @receiver the paused state.
 * @return the text of the confirmation dialog.
 */
fun GenState.Paused.discardWarning(): String =
    "${plural(remaining.size, "section")} left will not be generated, and the stored copy of “${book.title}” is deleted. ${keptNote()}"

/**
 * Says what happens to the deck when the rest of a paused generation is given up.
 *
 * @receiver the paused state.
 * @return that the cards stay in the library, or that there are none yet.
 */
private fun GenState.Paused.keptNote(): String =
    if (deckId == null) "Nothing was added to your library yet." else "The ${plural(cards, "card")} generated so far stay in your library."
