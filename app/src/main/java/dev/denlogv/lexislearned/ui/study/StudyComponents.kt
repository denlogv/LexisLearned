package dev.denlogv.lexislearned.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.Grade

/** The green used for right answers. */
val SuccessGreen = Color(0xFF2E7D32)

/**
 * The callbacks a study step can trigger.
 *
 * @property onGrade a single-card step was answered; AGAIN means wrong.
 * @property onIntroduced an introduction step was finished.
 * @property onKnown the learner knows the word already.
 * @property onPair a Pair board was finished, with the ids of the cards that had a wrong match.
 */
class StudyActions(val onGrade: (Grade) -> Unit, val onIntroduced: () -> Unit, val onKnown: () -> Unit, val onPair: (Set<Long>) -> Unit)

/**
 * The callbacks of a view model as [StudyActions].
 *
 * @receiver the study view model.
 * @return the actions bound to it.
 */
fun StudyViewModel.actions(): StudyActions = StudyActions(::answer, ::introduced, ::known, ::answerPair)

/**
 * The question of a step: the word and, if there is one, its pronunciation.
 *
 * @param card the word.
 * @param d which side is the question.
 */
@Composable
fun PromptText(card: CardEntity, d: Direction) = WordBlock(card.prompt(d), card.promptHint(d))

/**
 * Lays out a group of buttons: side by side below the content, or stacked beside it in landscape.
 *
 * @param vertical true to stack the buttons.
 * @param content the buttons; each gets the modifier that sizes it within the group.
 */
@Composable
fun ButtonGroup(vertical: Boolean, content: @Composable (Modifier) -> Unit) {
    if (vertical) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { content(Modifier.fillMaxWidth()) }
    } else {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content(Modifier.weight(1f))
        }
    }
}

/**
 * The three buttons that rate how well a word was remembered.
 *
 * @param onGrade called with the chosen grade.
 * @param vertical whether the buttons are stacked.
 */
@Composable
fun GradeButtons(onGrade: (Grade) -> Unit, vertical: Boolean = false) = ButtonGroup(vertical) { size ->
    OutlinedButton({ onGrade(Grade.AGAIN) }, size) { Text("Forgot") }
    FilledTonalButton({ onGrade(Grade.GOOD) }, size) { Text("Got it") }
    FilledTonalButton({ onGrade(Grade.EASY) }, size) { Text("Easy") }
}
