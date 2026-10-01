package dev.denlogv.lexislearned.domain

import kotlin.math.max
import kotlin.math.roundToInt

/** How well a word was remembered, used to schedule its next review. */
enum class Grade {
    /** Forgotten: the word starts over. */
    AGAIN,

    /** Remembered with difficulty. */
    HARD,

    /** Remembered. */
    GOOD,

    /** Remembered easily. */
    EASY,
}

/** Decides when a card is due next. */
interface Scheduler {
    /**
     * Computes the card's new state after a review.
     *
     * @param progress the state before the review.
     * @param grade how well the word was remembered.
     * @param now the current time in epoch milliseconds.
     * @return the state after the review, including the new due time.
     */
    fun review(progress: CardProgress, grade: Grade, now: Long): CardProgress
}

/** SM-2 variant with a ten-minute relearning step after a lapse. */
object Sm2Scheduler : Scheduler {
    private const val MINUTE = 60_000L
    private const val DAY = 24 * 60 * MINUTE
    private const val MIN_EASE = 1.3
    private const val RELEARN_MINUTES = 10
    private const val FIRST_INTERVAL_DAYS = 1
    private const val SECOND_INTERVAL_DAYS = 6
    private const val FIRST_EASY_DAYS = 4
    private const val SECOND_EASY_DAYS = 8

    /**
     * Applies SM-2: a lapse resets the word, otherwise the interval grows with the ease factor.
     *
     * @param progress the state before the review.
     * @param grade how well the word was remembered.
     * @param now the current time in epoch milliseconds.
     * @return the state after the review.
     */
    override fun review(progress: CardProgress, grade: Grade, now: Long): CardProgress {
        if (grade == Grade.AGAIN) return lapse(progress, now)
        val reps = progress.reps + 1
        val ease = easeAfter(progress.ease, grade)
        val interval = intervalAfter(reps, progress.intervalDays, ease, grade)
        return progress.copy(reps = reps, ease = ease, intervalDays = interval, dueAt = now + interval * DAY)
    }

    /**
     * The state after a failed review: progress restarts and the word returns after a short relearning step.
     *
     * @param progress the state before the review.
     * @param now the current time in epoch milliseconds.
     * @return the reset state, with a slightly lowered ease factor.
     */
    private fun lapse(progress: CardProgress, now: Long) = progress.copy(
        reps = 0,
        intervalDays = 0,
        ease = max(MIN_EASE, progress.ease - 0.2),
        dueAt = now + RELEARN_MINUTES * MINUTE,
        lapses = if (progress.reps > 0) progress.lapses + 1 else progress.lapses,
    )

    /**
     * The ease factor after a successful review.
     *
     * @param ease the current ease factor.
     * @param grade HARD lowers it, EASY raises it, GOOD keeps it.
     * @return the new ease factor, never below the minimum.
     */
    private fun easeAfter(ease: Double, grade: Grade): Double = when (grade) {
        Grade.HARD -> max(MIN_EASE, ease - 0.15)
        Grade.EASY -> ease + 0.15
        else -> ease
    }

    /**
     * The interval until the next review: fixed for the first two reviews, then multiplied by the ease factor.
     *
     * @param reps the review number after this one (1 for the first successful review).
     * @param previous the previous interval in days.
     * @param ease the ease factor after this review.
     * @param grade the grade of this review.
     * @return the new interval in days.
     */
    private fun intervalAfter(reps: Int, previous: Int, ease: Double, grade: Grade): Int = when (reps) {
        1 -> if (grade == Grade.EASY) FIRST_EASY_DAYS else FIRST_INTERVAL_DAYS
        2 -> if (grade == Grade.EASY) SECOND_EASY_DAYS else SECOND_INTERVAL_DAYS
        else -> {
            val factor = when (grade) {
                Grade.HARD -> 1.2
                Grade.EASY -> ease * 1.3
                else -> ease
            }
            max(previous + 1, (previous * factor).roundToInt())
        }
    }
}
