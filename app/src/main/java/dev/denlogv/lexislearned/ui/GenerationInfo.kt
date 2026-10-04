package dev.denlogv.lexislearned.ui

import dev.denlogv.lexislearned.ai.GenState

/**
 * What the info button of the status chip tells about a running generation.
 *
 * @receiver the running state.
 * @return the explanation: where the chapters go, and what each of the two pause buttons does.
 */
fun GenState.Running.info(): String =
    "Chapters appear in your library as soon as they are ready. “$PAUSE_AFTER_SECTION_LABEL” waits for the section in progress, " +
        "or for the first one if none has started yet; that request is paid for already. “$PAUSE_NOW_LABEL” gives the request " +
        "in flight up, and the section is done again on resume."

/**
 * What the info button of the status chip tells about a paused generation: what is kept, what each button does and what went wrong.
 *
 * @receiver the paused state.
 * @return the explanation, with the problems that stopped the generation, if any.
 */
fun GenState.Paused.info(): String {
    val what = "The chapters that are done are in your library and are not generated or paid for again. " +
        "${resumeLabel()} does only the sections that are left and adds them to the same deck; Choose sections lets you pick which " +
        "ones; the cross in the corner discards the rest and forgets the book while the deck stays in your library."
    return what + problems(failed)
}

/**
 * The problems that stopped a generation, as a list under the explanation.
 *
 * @param failed what went wrong, one entry per section or reason.
 * @return the list, or an empty text if nothing went wrong; only the first few entries are named.
 */
private fun problems(failed: List<String>): String {
    if (failed.isEmpty()) return ""
    val named = failed.take(MAX_LISTED_PROBLEMS).joinToString("\n") { "• $it" }
    val more = (failed.size - MAX_LISTED_PROBLEMS).takeIf { it > 0 }?.let { "\n… and $it more" }.orEmpty()
    return "\n\nWhat went wrong:\n$named$more\n\nCheck the API key, the model and the server in the settings (the gear icon), then retry."
}

/** How many problems the explanation of a paused generation names. */
private const val MAX_LISTED_PROBLEMS = 5

/**
 * What the info button of the status chip tells about a finished generation.
 *
 * @receiver the finished state.
 * @return the explanation.
 */
fun GenState.Finished.info(): String =
    "The deck, with ${plural(cards, "card")}, is in your library. Tap the card to open it, or the cross to dismiss it."

/**
 * What the info button of the status chip tells about a generation that failed.
 *
 * @receiver the failed state.
 * @return the explanation: what happened and what to try.
 */
fun GenState.Failed.info(): String =
    "Nothing was added to your library. $message\n\nCheck the API key, the model and the server in the settings (the gear icon), " +
        "then choose the book again with Add deck. Dismiss this card with the cross."
