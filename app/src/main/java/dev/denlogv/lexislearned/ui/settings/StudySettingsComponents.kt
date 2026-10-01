package dev.denlogv.lexislearned.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Settings
import kotlin.math.roundToInt

/**
 * A small grey explanation under a setting.
 *
 * @param text the explanation.
 */
@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * The settings that shape a study session.
 *
 * @param prefs the current settings.
 * @param settings where changes are saved.
 */
@Composable
fun StudySection(prefs: Prefs, settings: Settings) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Study sessions", style = MaterialTheme.typography.titleMedium)
        Text("Cards per session: ${prefs.sessionSize}")
        Slider(prefs.sessionSize.toFloat(), { settings.setSessionSize(it.roundToInt()) }, valueRange = 3f..30f, steps = 26)
        Text("New cards per session: ${prefs.newPerSession}")
        Slider(prefs.newPerSession.toFloat(), { settings.setNewPerSession(it.roundToInt()) }, valueRange = 0f..20f, steps = 19)
        Stepper(
            "Rounds per session",
            prefs.rounds,
            1..5,
            settings::setRounds,
            "How many times each word goes through all selected modes in one session.",
        )
        Stepper(
            "Sessions to complete a word",
            prefs.sessionsToComplete,
            1..10,
            settings::setSessionsToComplete,
            "A word is completed after this many successful sessions.",
        )
        SpacingSwitch(prefs.spaceSessions, settings::setSpaceSessions)
        Hint(sessionSummary(prefs))
    }
}

/**
 * The switch for spacing sessions over days.
 *
 * @param checked whether spacing is on.
 * @param onChange called with the new value.
 */
@Composable
private fun SpacingSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Space sessions over days")
            Hint("On: a word returns after a growing pause (spaced repetition). Off: it can be studied again right away.")
        }
        Switch(checked, onChange)
    }
}

/**
 * A labelled number with minus and plus buttons.
 *
 * @param label what the number is.
 * @param value the current value.
 * @param range the allowed values; the buttons stop at its ends.
 * @param onChange called with the new value.
 * @param hint an explanation under the control.
 */
@Composable
fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit, hint: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f))
            OutlinedButton({ onChange(value - 1) }, enabled = value > range.first) { Text("−") }
            Text("$value", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
            OutlinedButton({ onChange(value + 1) }, enabled = value < range.last) { Text("+") }
        }
        Hint(hint)
    }
}
