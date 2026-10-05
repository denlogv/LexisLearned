package dev.denlogv.lexislearned.ui.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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

/** Gap between two tiles, across and down. */
private val TILE_SPACING = 12.dp

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
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp), // so the last row clears the screen's edge
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Match each word with its translation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PairGrid(cards, right, d, state, { state = state.pickLeft(it) }) { state = state.pickRight(it) }
    }
}

/**
 * The tiles of a Pair board: each row holds a word on the left and one of the shuffled translations on the right.
 *
 * @param words the cards in the order of the words.
 * @param translations the same cards in the shuffled order of the translations.
 * @param d which side is shown on the left.
 * @param state the board state, which decides each tile's colour.
 * @param onWord called with a card's id when its word is tapped.
 * @param onTranslation called with a card's id when its translation is tapped.
 */
@Composable
private fun PairGrid(
    words: List<CardEntity>,
    translations: List<CardEntity>,
    d: Direction,
    state: PairState,
    onWord: (Long) -> Unit,
    onTranslation: (Long) -> Unit,
) {
    EqualTileGrid(columns = 2, spacing = TILE_SPACING) {
        words.zip(translations).forEach { (word, translation) ->
            PairTile(word.prompt(d), word.id in state.matched, selected = state.left == word.id, error = false) { onWord(word.id) }
            PairTile(translation.answer(d), translation.id in state.matched, selected = false, error = state.wrong == translation.id) {
                onTranslation(translation.id)
            }
        }
    }
}

/**
 * Lays tiles out in rows, every tile as wide as its column and as tall as the tallest tile on the board, so the board reads as a
 * grid however long each word or translation is. Children are placed row by row, left to right.
 *
 * @param columns how many tiles stand side by side.
 * @param spacing the gap between tiles, across and down.
 * @param content the tiles.
 */
@Composable
private fun EqualTileGrid(columns: Int, spacing: Dp, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = (constraints.maxWidth - gap * (columns - 1)) / columns
        val height = measurables.maxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0
        val tiles = measurables.map { it.measure(Constraints.fixed(width, height)) }
        val rows = (tiles.size + columns - 1) / columns
        layout(constraints.maxWidth, (rows * (height + gap) - gap).coerceAtLeast(0)) {
            tiles.forEachIndexed { i, tile -> tile.place(i % columns * (width + gap), i / columns * (height + gap)) }
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
        Box(Modifier.fillMaxSize().padding(8.dp), Alignment.Center) {
            Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
        }
    }
}
