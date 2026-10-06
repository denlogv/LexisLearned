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
 * A Type step: the learner types the answer, checks it and moves on.
 *
 * @param card the word.
 * @param d which side is the question.
 * @param onGrade called with GOOD or AGAIN when the learner moves on.
 */
@Composable
fun TypeCard(card: CardEntity, d: Direction, onGrade: (Grade) -> Unit) {
    var state by remember { mutableStateOf(TypeState()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    HideKeyboardOnceChecked(state.checked)
    val submit = { if (state.checked) onGrade(state.grade) else state = state.check(card.answer(d)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = QUESTION_MIN_HEIGHT).padding(vertical = 24.dp), Alignment.Center) { PromptText(card, d) }
        OutlinedTextField(
            state.text,
            { state = state.withText(it) },
            Modifier.fillMaxWidth().focusRequester(focus),
            label = { Text("Type the answer") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            isError = state.result == false,
        )
        TypeFeedback(state, card, d)
        Button(submit, Modifier.fillMaxWidth(), enabled = state.canSubmit) { Text(if (state.checked) "Next" else "Check") }
        if (!state.checked) OutlinedButton({ state = state.giveUp() }, Modifier.fillMaxWidth()) { Text("I don't know") }
    }
}

/**
 * Hides the keyboard and drops the focus from the answer field as soon as the answer is checked, so the feedback and the "Next"
 * button are not covered. The next step is a new [TypeCard], which asks for focus again and brings the keyboard back.
 *
 * @param checked whether the answer has been checked or given up.
 */
@Composable
private fun HideKeyboardOnceChecked(checked: Boolean) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(checked) {
        if (checked) {
            keyboard?.hide()
            focusManager.clearFocus()
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
