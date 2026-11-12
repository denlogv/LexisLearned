package dev.denlogv.lexislearned.ui.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
 * One compact chip per study mode, so that all five fit on one line of a phone. On a narrower screen or with a larger font
 * the row wraps instead of scrolling, so no chip is ever hidden off screen.
 *
 * @param selected the selected modes.
 * @param onToggle called when a chip is tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeChips(selected: Set<StudyMode>, onToggle: (StudyMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StudyMode.entries.forEach { mode -> ModeChip(mode.label(), mode in selected) { onToggle(mode) } }
    }
}

/**
 * A chip that looks like a [androidx.compose.material3.FilterChip] but has less padding around its text; the material chip
 * has fixed, wide padding, which made the five mode chips too wide for one line of a phone.
 *
 * @param label the text on the chip.
 * @param selected whether the mode is selected.
 * @param onClick called when the chip is tapped.
 */
@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.minimumInteractiveComponentSize().semantics { role = Role.Checkbox },
        shape = FilterChipDefaults.shape,
        color = if (selected) colors.secondaryContainer else Color.Transparent,
        contentColor = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        border = if (selected) null else FilterChipDefaults.filterChipBorder(enabled = true, selected = false),
    ) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}
