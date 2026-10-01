package dev.denlogv.lexislearned.ui.deck

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.DeckEntity
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.StudyMode
import dev.denlogv.lexislearned.ui.description
import dev.denlogv.lexislearned.ui.label

/**
 * The direction and mode choices shown on the book, part and chapter screens.
 *
 * @param deck the book, for the language pair; nothing is shown until it is loaded.
 * @param prefs the current settings.
 * @param onDirection called when a direction is chosen.
 * @param onToggleMode called when a mode chip is tapped.
 */
@Composable
fun StudyOptions(deck: DeckEntity?, prefs: Prefs, onDirection: (Direction) -> Unit, onToggleMode: (StudyMode) -> Unit) {
    if (deck == null) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Direction", style = MaterialTheme.typography.labelLarge)
        DirectionChips(deck, prefs.direction, onDirection)
        Text("Modes (every word goes through all selected)", style = MaterialTheme.typography.labelLarge)
        ModeChips(prefs.modes, onToggleMode)
        Text(
            prefs.modes.joinToString(" · ") { "${it.label()}: ${it.description()}" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The two direction chips, for example "EN → RU" and "RU → EN".
 *
 * @param deck the book, for the two language codes.
 * @param selected the current direction.
 * @param onSelect called when a chip is tapped.
 */
@Composable
private fun DirectionChips(deck: DeckEntity, selected: Direction, onSelect: (Direction) -> Unit) {
    val front = deck.frontLang.uppercase()
    val back = deck.backLang.uppercase()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected == Direction.FORWARD, { onSelect(Direction.FORWARD) }, { Text("$front → $back") })
        FilterChip(selected == Direction.REVERSE, { onSelect(Direction.REVERSE) }, { Text("$back → $front") })
    }
}

/**
 * One chip per study mode, on a horizontally scrolling row.
 *
 * @param selected the selected modes.
 * @param onToggle called when a chip is tapped.
 */
@Composable
private fun ModeChips(selected: Set<StudyMode>, onToggle: (StudyMode) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StudyMode.entries.forEach { mode ->
            FilterChip(selected = mode in selected, onClick = { onToggle(mode) }, label = { Text(mode.label()) })
        }
    }
}
