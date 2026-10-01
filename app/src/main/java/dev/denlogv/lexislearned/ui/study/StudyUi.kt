package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.domain.StudyMode
import dev.denlogv.lexislearned.ui.label
import dev.denlogv.lexislearned.ui.plural

/**
 * Everything the study screen shows.
 *
 * @property loading true until the session's words are loaded.
 * @property mode the mode of the current step.
 * @property exposure true for a Learn step that only introduces a word (when several modes are selected); it is not graded.
 * @property current the cards on screen: one, or several for Pair.
 * @property decoyIds ids of filler cards among [current]; they are shown on the Pair board but never graded.
 * @property options the answer choices of a Select step.
 * @property stepsDone steps finished so far.
 * @property stepsLeft steps still to do, including repeats of wrong answers.
 * @property totalWords how many words are in the session.
 * @property completedWords words that finished all their steps.
 * @property correct answers that were right.
 * @property wrong answers that were wrong.
 * @property finished true when every word has finished all its steps.
 */
data class StudyUi(
    val loading: Boolean = true,
    val mode: StudyMode = StudyMode.LEARN,
    val exposure: Boolean = false,
    val current: List<CardEntity> = emptyList(),
    val decoyIds: Set<Long> = emptySet(),
    val options: List<String> = emptyList(),
    val stepsDone: Int = 0,
    val stepsLeft: Int = 0,
    val totalWords: Int = 0,
    val completedWords: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val finished: Boolean = false,
) {
    /** Progress through the session from 0 to 1. */
    val progress: Float get() = if (stepsDone + stepsLeft == 0) 0f else stepsDone.toFloat() / (stepsDone + stepsLeft)

    /** The title of the top bar: the current mode while studying, otherwise just "Study". */
    val title: String get() = if (loading || finished) "Study" else mode.label()

    /** The line under the progress bar, for example "12 steps left · 2/5 words done". */
    val statusLine: String get() = "${plural(
        stepsLeft,
        "step",
    )} left · $completedWords/$totalWords ${if (totalWords == 1) "word" else "words"} done"
}
