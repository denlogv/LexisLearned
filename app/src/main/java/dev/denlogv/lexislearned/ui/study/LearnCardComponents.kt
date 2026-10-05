package dev.denlogv.lexislearned.ui.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.ui.cardClickable

/** Rotation in degrees past which the back of a flipping card is shown. */
private const val HALF_TURN = 90f

/** Full rotation of a flipped card in degrees. */
private const val FLIPPED = 180f

/**
 * Learn and Check steps: tap the card to flip it, then continue or rate how well the word was known.
 *
 * @param card the word.
 * @param d which side is the question.
 * @param showExamples whether example sentences are shown (Learn) or not (Check).
 * @param exposure whether this step only introduces the word, with "I know it" and "Next" instead of a rating.
 * @param actions what the buttons do.
 */
@Composable
fun LearnCard(card: CardEntity, d: Direction, showExamples: Boolean, exposure: Boolean, actions: StudyActions) {
    var flipped by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (flipped) FLIPPED else 0f, tween(FLIP_MS), label = "flip")
    val face: @Composable (Modifier) -> Unit = { modifier ->
        FlipCard(
            rotation,
            modifier,
            onFlip = {
                flipped = !flipped
                revealed = true
            },
            front = { CardFront(card, d, showExamples, revealed) },
            back = { CardBack(card, d, showExamples) },
        )
    }
    CardWithActions(face) { vertical -> LearnActions(revealed, exposure, actions, vertical) }
}

/**
 * Places the card and its buttons: the buttons below the card, or beside it in landscape, where a card below the top bar and
 * above a row of buttons would be too short to read.
 *
 * @param card the card; gets the modifier that sizes it.
 * @param actions the buttons; told whether they are stacked beside the card.
 */
@Composable
private fun CardWithActions(card: @Composable (Modifier) -> Unit, actions: @Composable (Boolean) -> Unit) {
    if (isLandscape()) {
        Row(
            Modifier.fillMaxSize().padding(bottom = 16.dp), // nothing below the card in landscape keeps it off the screen's edge
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            card(Modifier.weight(1f).fillMaxHeight())
            Box(Modifier.width(SIDE_ACTIONS_WIDTH)) { actions(true) }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            card(Modifier.weight(1f))
            actions(false)
        }
    }
}

/** Width of the button column beside the card in landscape. */
private val SIDE_ACTIONS_WIDTH = 180.dp

/** How long flipping a card takes, in milliseconds. */
private const val FLIP_MS = 350

/**
 * A card that can be flipped around its vertical axis, showing the face that is turned towards the viewer.
 *
 * @param rotation the current rotation in degrees, 0 for the front and 180 for the back.
 * @param modifier layout modifier of the card.
 * @param onFlip called when the card is tapped.
 * @param front the content of the front.
 * @param back the content of the back.
 */
@Composable
private fun FlipCard(rotation: Float, modifier: Modifier, onFlip: () -> Unit, front: @Composable () -> Unit, back: @Composable () -> Unit) {
    val showBack = rotation > HALF_TURN
    val container = if (showBack) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier.fillMaxWidth().padding(top = 8.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = CAMERA_DISTANCE * density
            }
            .cardClickable(onClick = onFlip),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Box(
            Modifier.fillMaxSize().padding(24.dp).graphicsLayer { rotationY = if (showBack) FLIPPED else 0f },
            contentAlignment = Alignment.Center,
        ) { if (showBack) back() else front() }
    }
}

/** Camera distance factor that keeps the flip from looking distorted. */
private const val CAMERA_DISTANCE = 12f

/**
 * The front of a card: the question with an optional example and a hint to tap.
 *
 * @param card the word.
 * @param d which side is the question.
 * @param showExamples whether the example sentence is shown.
 * @param revealed whether the card was flipped before, which changes the hint text.
 */
@Composable
private fun CardFront(card: CardEntity, d: Direction, showExamples: Boolean, revealed: Boolean) {
    CardFace(card.prompt(d), card.promptHint(d), otherWord = card.answer(d)) {
        if (showExamples) card.promptExample(d)?.let { Example(it) }
        Hint(if (revealed) "Tap to flip" else "Tap to reveal")
    }
}

/**
 * The back of a card: the answer with an optional example and a hint to tap.
 *
 * @param card the word.
 * @param d which side is the question.
 * @param showExamples whether the example sentence is shown.
 */
@Composable
private fun CardBack(card: CardEntity, d: Direction, showExamples: Boolean) {
    CardFace(card.answer(d), card.answerHint(d), otherWord = card.prompt(d)) {
        if (showExamples) card.answerExample(d)?.let { Example(it) }
        Hint("Tap to flip back", MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/**
 * An example sentence.
 *
 * @param text the sentence.
 */
@Composable
private fun Example(text: String) {
    Text(text, Modifier.padding(top = 20.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
}

/**
 * A small hint line under the card content.
 *
 * @param text the hint.
 * @param color the text colour.
 */
@Composable
private fun Hint(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, Modifier.padding(top = 32.dp), style = MaterialTheme.typography.labelMedium, color = color)
}

/**
 * The buttons next to the card: nothing until it was flipped, then rating buttons or "I know it" and "Next".
 *
 * @param revealed whether the card was flipped at least once.
 * @param exposure whether the step only introduces the word.
 * @param actions what the buttons do.
 * @param vertical whether the buttons are stacked beside the card (landscape) instead of in a row below it.
 */
@Composable
private fun LearnActions(revealed: Boolean, exposure: Boolean, actions: StudyActions, vertical: Boolean) {
    when {
        !revealed -> if (!vertical) Spacer(Modifier.height(80.dp))
        exposure -> ButtonGroup(vertical) { size ->
            OutlinedButton(actions.onKnown, size) { Text("I know it") }
            Button(actions.onIntroduced, size) { Text("Next") }
        }
        else -> GradeButtons(actions.onGrade, vertical)
    }
}
