package dev.denlogv.lexislearned.data

/**
 * A deck with its progress, as shown in the library.
 *
 * [completed] words have had at least [needed] sessions. [units] adds up min(sessions, needed) over all words, so
 * [progress] grows smoothly from session to session.
 *
 * @property id database id.
 * @property title title in the learned language.
 * @property nativeTitle title in the native language, if known.
 * @property total number of words.
 * @property completed words that have reached the required number of sessions.
 * @property inProgress words that have had some sessions but not enough yet.
 * @property units the sum over all words of min(sessions, needed).
 * @property needed the number of sessions that completes a word.
 * @property due words that are due for review now.
 */
data class DeckSummary(
    val id: Long,
    val title: String,
    val nativeTitle: String?,
    val total: Int,
    val completed: Int,
    val inProgress: Int,
    val units: Int,
    val needed: Int,
    val due: Int,
) {
    /** Overall progress from 0 to 1. */
    val progress: Float get() = if (total == 0 || needed == 0) 0f else units.toFloat() / (total * needed)
}

/**
 * A chapter with its progress, as shown in lists (see [DeckSummary] for the meaning of the numbers).
 *
 * @property id database id.
 * @property partId the part the chapter belongs to, or null.
 * @property title title in the learned language.
 * @property nativeTitle title in the native language, if known.
 * @property total number of words.
 * @property completed words that have reached the required number of sessions.
 * @property inProgress words that have had some sessions but not enough yet.
 * @property units the sum over all words of min(sessions, needed).
 * @property needed the number of sessions that completes a word.
 * @property due words that are due for review now.
 */
data class ChapterSummary(
    val id: Long,
    val partId: Long?,
    val title: String,
    val nativeTitle: String?,
    val total: Int,
    val completed: Int,
    val inProgress: Int,
    val units: Int,
    val needed: Int,
    val due: Int,
) {
    /** Progress from 0 to 1. */
    val progress: Float get() = if (total == 0 || needed == 0) 0f else units.toFloat() / (total * needed)
}

/**
 * A card's study state keyed by its source id, used to keep progress when a deck is imported again.
 *
 * @property sourceId the card's id in its source file.
 * @property reps consecutive successful reviews.
 * @property ease SM-2 ease factor.
 * @property intervalDays current review interval in days.
 * @property dueAt epoch milliseconds when the card is due, or null.
 * @property lapses how often the card was failed.
 * @property sessionsDone sessions the word has completed.
 */
data class ProgressRow(
    val sourceId: String,
    val reps: Int,
    val ease: Double,
    val intervalDays: Int,
    val dueAt: Long?,
    val lapses: Int,
    val sessionsDone: Int,
)
