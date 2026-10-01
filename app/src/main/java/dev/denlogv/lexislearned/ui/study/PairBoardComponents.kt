package dev.denlogv.lexislearned.ui.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.domain.Direction
import kotlinx.coroutines.delay

/** How long a wrong pick stays red, in milliseconds. */
private const val WRONG_FLASH_MS = 500L

/** Pause after the last match before moving on, in milliseconds. */
private const val DONE_PAUSE_MS = 400L

/** Opacity of the green behind a matched tile. */
private const val MATCHED_ALPHA = 0.25f

/** Smallest height of a tile. */
private val TILE_MIN_HEIGHT = 72.dp

/**
 * A Pair step: words on the left, their shuffled translations on the right; tap a word, then its translation.
 *
 * @param cards the cards on the board, fillers included.
 * @param d which side is shown on the left.
 * @param onDone called with the ids of cards that had a wrong match, once all pairs are matched.
 */
@Composable
fun PairBoard(cards: List<CardEntity>, d: Direction, onDone: (Set<Long>) -> Unit) {
    val right = remember(cards) { cards.shuffled() }
    var state by remember(cards) { mutableStateOf(PairState(cards.map { it.id })) }
    LaunchedEffect(state.wrong) {
        if (state.wrong != null) {
            delay(WRONG_FLASH_MS)
            state = state.clearWrong()
        }
    }
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            delay(DONE_PAUSE_MS)
            onDone(state.missed)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Match each word with its translation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TileColumn(Modifier.weight(1f), cards, { it.prompt(d) }, state) { state = state.pickLeft(it) }
            TileColumn(Modifier.weight(1f), right, { it.answer(d) }, state, isRight = true) { state = state.pickRight(it) }
        }
    }
}

/**
 * One column of tiles.
 *
 * @param modifier layout modifier.
 * @param cards the cards in this column's order.
 * @param text the text of a card's tile.
 * @param state the board state, which decides each tile's colour.
 * @param isRight true for the translations column, which shows wrong picks in red instead of selections.
 * @param onPick called with a card's id when its tile is tapped.
 */
@Composable
private fun TileColumn(
    modifier: Modifier,
    cards: List<CardEntity>,
    text: (CardEntity) -> String,
    state: PairState,
    isRight: Boolean = false,
    onPick: (Long) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.forEach { card ->
            PairTile(
                text(card),
                matched = card.id in state.matched,
                selected = !isRight && state.left == card.id,
                error = isRight && state.wrong == card.id,
            ) { onPick(card.id) }
        }
    }
}

/**
 * One tile of the board.
 *
 * @param text the word or translation.
 * @param matched whether the tile was matched; it turns green and can no longer be tapped.
 * @param selected whether it is the selected word.
 * @param error whether it was just picked wrongly.
 * @param onClick called when the tile is tapped.
 */
@Composable
private fun PairTile(text: String, matched: Boolean, selected: Boolean, error: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val target = when {
        matched -> SuccessGreen.copy(alpha = MATCHED_ALPHA)
        error -> scheme.errorContainer
        selected -> scheme.primaryContainer
        else -> scheme.surfaceVariant
    }
    val background by animateColorAsState(target, label = "tile")
    Card(
        Modifier.fillMaxWidth().heightIn(min = TILE_MIN_HEIGHT).clickable(enabled = !matched, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = TILE_MIN_HEIGHT).padding(8.dp), Alignment.Center) {
            Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
        }
    }
}
