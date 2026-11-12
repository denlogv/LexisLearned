package dev.denlogv.lexislearned.ui

import dev.denlogv.lexislearned.domain.StudyMode

/**
 * A number followed by the singular or plural form of a word, for example "1 word" or "3 words".
 *
 * @param n the number.
 * @param one the singular form.
 * @param many the plural form; defaults to the singular with an "s".
 * @return the number and the matching form.
 */
fun plural(n: Int, one: String, many: String = "${one}s"): String = "$n ${if (n == 1) one else many}"

/**
 * A duration in whole minutes for an estimate, for example "~4 min".
 *
 * @param millis the duration in milliseconds.
 * @return "<1 min" under half a minute, otherwise the minutes rounded to the nearest one.
 */
fun minutesLabel(millis: Long): String {
    val minutes = (millis + MILLIS_PER_MINUTE / 2) / MILLIS_PER_MINUTE
    return if (minutes < 1) "<1 min" else "~$minutes min"
}

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * The progress line shown under bars, for example "3/16 completed · 5 in progress · 33% · 2 due". Parts that are zero
 * are left out.
 *
 * @param completed words that are completed.
 * @param total all words.
 * @param inProgress words with some but not all required sessions.
 * @param progress overall progress from 0 to 1.
 * @param due words due for review.
 * @param extra text put in front, for example "3 chapters"; null for none.
 * @return the line.
 */
fun progressLabel(completed: Int, total: Int, inProgress: Int, progress: Float, due: Int, extra: String? = null): String = listOfNotNull(
    extra,
    "$completed/$total completed",
    if (inProgress > 0) "$inProgress in progress" else null,
    "${(progress * PERCENT).toInt()}%",
    if (due > 0) "$due due" else null,
).joinToString(" · ")

private const val PERCENT = 100

/**
 * The text of a study button.
 *
 * @param scope what is studied: "book", "part" or "chapter".
 * @param due how many words are due.
 * @param wholeChapter whether a session is one whole chapter. A book or part then offers its next chapter, not all its due
 * words, so the due count would promise more than the session holds and is left out; a chapter's own count stays true.
 * @return for example "Study book (3 due + new)", "Study chapter (new cards)" or "Study part (next chapter)".
 */
fun studyButtonLabel(scope: String, due: Int, wholeChapter: Boolean = false): String = when {
    wholeChapter && scope != CHAPTER_SCOPE -> "Study $scope (next chapter)"
    due > 0 -> "Study $scope ($due due + new)"
    else -> "Study $scope (new cards)"
}

/** The scope name of a study button that starts a session inside one chapter. */
const val CHAPTER_SCOPE = "chapter"

/**
 * The short name of a study mode.
 *
 * @receiver the mode.
 * @return the name shown on chips and titles.
 */
fun StudyMode.label(): String = when (this) {
    StudyMode.LEARN -> "Learn"
    StudyMode.PAIR -> "Pair"
    StudyMode.SELECT -> "Select"
    StudyMode.CHECK -> "Check"
    StudyMode.TYPE -> "Type"
}

/**
 * A few words on what a study mode does.
 *
 * @receiver the mode.
 * @return the description shown under the mode chips.
 */
fun StudyMode.description(): String = when (this) {
    StudyMode.LEARN -> "see the word with examples"
    StudyMode.PAIR -> "match words with translations"
    StudyMode.SELECT -> "pick the right answer"
    StudyMode.CHECK -> "recall, reveal, rate yourself"
    StudyMode.TYPE -> "type the answer"
}
