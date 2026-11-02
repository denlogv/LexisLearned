package dev.denlogv.lexislearned.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.Grade

/** Smallest height of the area that holds the question, so the layout does not jump. */
private val QUESTION_MIN_HEIGHT = 140.dp

/**
 * A Type step: the learner types the answer, checks it and moves on. With a keyboard, Enter in the field checks the answer,
 * Ctrl+D gives up, and once the answer is checked Enter or Space moves on, see [shortcutFor].
 *
 * @param card the word.
 * @param d which side is the question.
 * @param onGrade called with GOOD or AGAIN when the learner moves on.
 */
@Composable
fun TypeCard(card: CardEntity, d: Direction, onGrade: (Grade) -> Unit) {
    var state by remember { mutableStateOf(TypeState()) }
    val field = remember { FocusRequester() }
    val step = remember { FocusRequester() }
    LaunchedEffect(Unit) { field.requestFocus() }
    HandOverFocusOnceChecked(state.checked, step)
    // The keys act on the state as it was composed: a press that the field used to check the answer may still reach the step,
    // and must not count as the press that moves on.
    val composed = state
    val keys = Modifier.stepKeys(step) { key ->
        composed.shortcutFor(key)?.perform({ state = composed.giveUp() }, { onGrade(composed.grade) }) != null
    }
    val submit = { if (state.checked) onGrade(state.grade) else state = state.check(card.answer(d)) }
    Column(keys.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = QUESTION_MIN_HEIGHT).padding(vertical = 24.dp), Alignment.Center) { PromptText(card, d) }
        AnswerField(state, field, { state = state.withText(it) }, submit)
        TypeFeedback(state, card, d)
        TypeButtons(state, submit) { state = state.giveUp() }
    }
}

/**
 * The field the answer is typed in; Enter checks it.
 *
 * @param state the typing state.
 * @param focus what gives the field the focus.
 * @param onText called with the new text when it is typed.
 * @param onSubmit called when Enter or the keyboard's "Done" is pressed.
 */
@Composable
private fun AnswerField(state: TypeState, focus: FocusRequester, onText: (String) -> Unit, onSubmit: () -> Unit) {
    OutlinedTextField(
        state.text,
        onText,
        Modifier.fillMaxWidth().focusRequester(focus),
        label = { Text("Type the answer") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        isError = state.result == false,
    )
}

/**
 * The buttons under the answer: "Check" (then "Next") and, until the answer is checked, "I don't know".
 *
 * @param state the typing state.
 * @param onSubmit called when "Check" or "Next" is pressed.
 * @param onGiveUp called when "I don't know" is pressed.
 */
@Composable
private fun TypeButtons(state: TypeState, onSubmit: () -> Unit, onGiveUp: () -> Unit) {
    Button(onSubmit, Modifier.fillMaxWidth(), enabled = state.canSubmit) { Text(if (state.checked) hinted("Next", "Enter") else "Check") }
    if (!state.checked) OutlinedButton(onGiveUp, Modifier.fillMaxWidth()) { Text(hinted("I don't know", "Ctrl+D")) }
}

/**
 * Hides the keyboard as soon as the answer is checked, so the feedback and the "Next" button are not covered, and hands the
 * focus from the answer field to the step. Dropping the focus altogether would leave it nowhere, so that the next key press
 * goes to the back button at the top of the screen. The next step is a new [TypeCard], which asks for focus again and brings
 * the keyboard back.
 *
 * @param checked whether the answer has been checked or given up.
 * @param step the focus of the step, see [stepKeys].
 */
@Composable
private fun HandOverFocusOnceChecked(checked: Boolean, step: FocusRequester) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(checked) {
        if (checked) {
            keyboard?.hide()
            focusManager.clearFocus()
            step.requestFocus()
        }
    }
}

/**
 * The result of a check: "Correct!" or the right answer, plus the example sentence in both languages.
 *
 * @param state the typing state; nothing is shown before the answer is checked.
 * @param card the word.
 * @param d which side is the question.
 */
@Composable
private fun TypeFeedback(state: TypeState, card: CardEntity, d: Direction) {
    val ok = state.result ?: return
    Text(
        if (ok) "Correct!" else "Correct answer: ${card.answer(d)}",
        color = if (ok) SuccessGreen else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.titleMedium,
    )
    card.revealedExamples(d).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
}
