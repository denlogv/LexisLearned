package dev.denlogv.lexislearned.domain

/** Turns a word's performance in a session into a scheduling grade and decides whether the session counts. */
object SessionGrading {
    private const val PERFECT = 1.0
    private const val GOOD_THRESHOLD = 0.8
    private const val HARD_THRESHOLD = 0.6

    /**
     * Maps a card's first-try accuracy in a session to a grade.
     *
     * @param accuracy share of graded steps answered correctly on the first try, or null if nothing was graded.
     * @param knew true if the learner marked the word as already known.
     * @return EASY for known or perfect words, GOOD from 80%, HARD from 60%, otherwise AGAIN (GOOD if nothing was graded).
     */
    fun gradeFor(accuracy: Double?, knew: Boolean): Grade = when {
        knew -> Grade.EASY
        accuracy == null -> Grade.GOOD
        accuracy >= PERFECT -> Grade.EASY
        accuracy >= GOOD_THRESHOLD -> Grade.GOOD
        accuracy >= HARD_THRESHOLD -> Grade.HARD
        else -> Grade.AGAIN
    }

    /**
     * Whether a session with this grade counts toward completing the word.
     *
     * @param grade the session's grade.
     * @return true unless the session went badly (AGAIN).
     */
    fun counts(grade: Grade): Boolean = grade != Grade.AGAIN
}
