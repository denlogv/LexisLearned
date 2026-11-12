package dev.denlogv.lexislearned.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import dev.denlogv.lexislearned.data.PAIR_SIZE_RANGE
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.data.TIME_LEFT_AFTER_RANGE
import kotlin.math.roundToInt

/** How many cards one study session may hold. */
private val SESSION_SIZE_RANGE = 3..30

/** How many unseen cards one study session may add. */
private val NEW_PER_SESSION_RANGE = 0..20

/** How many times a session may repeat each word. */
private val ROUNDS_RANGE = 1..5

/** How many successful sessions a word may need to count as completed. */
private val SESSIONS_TO_COMPLETE_RANGE = 1..10

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
 * The settings that shape a study session, in groups: how long it is, the Pair board, when a word counts as learned and the
 * estimate of the time left.
 *
 * @param prefs the current settings.
 * @param settings where changes are saved.
 */
@Composable
fun StudySettings(prefs: Prefs, settings: Settings) {
    SettingsGroup("Session length") { SessionLength(prefs, settings) }
    SettingsGroup("Pair mode") {
        Stepper(
            "Pairs per board",
            prefs.pairSize,
            PAIR_SIZE_RANGE,
            settings::setPairSize,
            "How many words are matched at once. When fewer are due, the board is filled up with words you have studied.",
        )
    }
    SettingsGroup("Progress") {
        Stepper(
            "Sessions to complete a word",
            prefs.sessionsToComplete,
            SESSIONS_TO_COMPLETE_RANGE,
            settings::setSessionsToComplete,
            "A word is completed after this many successful sessions.",
        )
        SwitchSetting(
            "Space sessions over days",
            "On: a word returns after a growing pause (spaced repetition). Off: it can be studied again right away.",
            prefs.spaceSessions,
            settings::setSpaceSessions,
        )
    }
    SettingsGroup("Time left") { TimeLeftSettings(prefs, settings) }
}

/**
 * The controls for the estimate of the time left: whether it is shown, and after how many steps.
 *
 * @receiver the group the controls are placed in.
 * @param prefs the current settings.
 * @param settings where changes are saved.
 */
@Composable
private fun ColumnScope.TimeLeftSettings(prefs: Prefs, settings: Settings) {
    SwitchSetting(
        "Show time left",
        "Under the progress bar: the minutes it takes to finish the session and the chapter at your pace.",
        prefs.showTimeLeft,
        settings::setShowTimeLeft,
    )
    if (prefs.showTimeLeft) {
        Stepper(
            "Steps before it shows",
            prefs.timeLeftAfter,
            TIME_LEFT_AFTER_RANGE,
            settings::setTimeLeftAfter,
            "A longer wait gives a steadier estimate; after the first step it is only a rough guess.",
        )
    }
}

/**
 * The controls for the length of a session: its cards, how many of them are new and how often each is repeated.
 *
 * @receiver the group the controls are placed in.
 * @param prefs the current settings.
 * @param settings where changes are saved.
 */
@Composable
private fun ColumnScope.SessionLength(prefs: Prefs, settings: Settings) {
    SliderSetting("Cards per session", prefs.sessionSize, SESSION_SIZE_RANGE, settings::setSessionSize)
    SliderSetting("New cards per session", prefs.newPerSession, NEW_PER_SESSION_RANGE, settings::setNewPerSession)
    Stepper(
        "Rounds per session",
        prefs.rounds,
        ROUNDS_RANGE,
        settings::setRounds,
        "How many times each word goes through all selected modes in one session.",
    )
    Hint(sessionSummary(prefs))
}

/**
 * A labelled number that is picked on a slider.
 *
 * @param label what the number is; the current value is added to it.
 * @param value the current value.
 * @param range the allowed values; the slider has a stop for each.
 * @param onChange called with the new value.
 */
@Composable
private fun SliderSetting(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Text("$label: $value")
        Slider(
            value.toFloat(),
            { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
        )
    }
}

/**
 * A labelled on/off switch with an explanation.
 *
 * @param label what the switch is.
 * @param hint an explanation under the label.
 * @param checked whether it is on.
 * @param onChange called with the new value.
 */
@Composable
private fun SwitchSetting(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Hint(hint)
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
