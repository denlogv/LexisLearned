package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.StudyMode

/** Answers the current step the way a perfect learner would and waits until the next one is shown. */
suspend fun StudyViewModel.playStep() {
    val current = ui.value
    answerLike(current, correct = true)
    awaitNextStep(current)
}

/** Answers the current step; a wrong answer is [Grade.AGAIN], a Pair step misses its first card. */
fun StudyViewModel.answerLike(current: StudyUi, correct: Boolean) {
    val grade = if (correct) Grade.GOOD else Grade.AGAIN
    when (current.mode) {
        StudyMode.LEARN -> if (current.exposure) introduced() else answer(grade)
        StudyMode.PAIR -> answerPair(if (correct) emptySet() else setOf(current.current.first().id))
        else -> answer(grade)
    }
}

/** Waits until the step after [shown] is on screen: the count of finished steps and the step reach the screen together. */
suspend fun StudyViewModel.awaitNextStep(shown: StudyUi) {
    ui.await { it.stepsDone > shown.stepsDone }
}

/** Plays steps until the session is over. */
suspend fun StudyViewModel.playToEnd() {
    while (!ui.value.finished) playStep()
}
