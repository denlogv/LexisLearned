package dev.denlogv.lexislearned.ui.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.Grade

/** Opacity of the green behind the right answer. */
private const val CORRECT_ALPHA = 0.85f

/**
 * A Select step: the question and several answers to pick from; after picking, the right answer turns green and "Next" appears.
 *
 * @param card the word.
 * @param d which side is the question.
 * @param options the answer choices, including the right one.
 * @param onGrade called with GOOD or AGAIN when the learner moves on.
 */
@Composable
fun SelectCard(card: CardEntity, d: Direction, options: List<String>, onGrade: (Grade) -> Unit) {
    val correct = card.answer(d)
    var picked by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { PromptText(card, d) }
        options.forEach { option ->
            OptionCard(option, optionState(option, picked, correct)) { picked = option }
        }
        Button(
            { onGrade(if (picked == correct) Grade.GOOD else Grade.AGAIN) },
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            enabled = picked != null,
        ) { Text("Next") }
    }
}

/**
 * One answer choice.
 *
 * @param text the choice.
 * @param state how it looks; only an open choice can be tapped.
 * @param onPick called when the choice is tapped.
 */
@Composable
private fun OptionCard(text: String, state: OptionState, onPick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val target = when (state) {
        OptionState.CORRECT -> SuccessGreen.copy(alpha = CORRECT_ALPHA)
        OptionState.WRONG -> scheme.errorContainer
        else -> scheme.surfaceVariant
    }
    val background by animateColorAsState(target, label = "option")
    Card(
        Modifier.fillMaxWidth().clickable(enabled = state == OptionState.OPEN, onClick = onPick),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Text(
            text,
            Modifier.padding(16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = if (state == OptionState.CORRECT || state == OptionState.WRONG) Color.White else scheme.onSurfaceVariant,
        )
    }
}
