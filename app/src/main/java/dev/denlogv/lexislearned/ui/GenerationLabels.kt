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
 * The text of the button that pauses a running generation. The first press waits for the current section, whose request is paid
 * for already; pressing again, once it says so, gives the request up at once.
 *
 * @receiver the running state.
 * @return "Pause", or "Pause now" when a pause was asked for already.
 */
fun GenState.Running.pauseLabel(): String = if (pausing) "Pause now" else "Pause"

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
