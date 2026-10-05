package dev.denlogv.lexislearned.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.denlogv.lexislearned.ai.Prompts
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.domain.CefrLevel

/**
 * The level, the card rules of that level and extra instructions for deck generation, each in a group of its own.
 *
 * @param prefs the current settings.
 * @param settings where changes are saved.
 */
@Composable
fun PromptSettings(prefs: Prefs, settings: Settings) {
    SettingsGroup("Your level") {
        LevelChips(prefs.level, settings::setLevel)
        Hint(
            "${prefs.level.label} · ${prefs.level.title}: cards cover vocabulary above this level. " +
                "Each level has its own card rules.",
        )
    }
    SettingsGroup("Card rules") {
        RulesEditor(prefs, settings)
    }
    SettingsGroup("Extra instructions") {
        ExtraInstructions(prefs.extraInstructions, settings::setExtraInstructions)
    }
}

/**
 * The six levels as one row of equal buttons that always spans the available width, so none is cut off or needs scrolling.
 *
 * @param level the selected level.
 * @param onPick called with the tapped level.
 */
@Composable
fun LevelChips(level: CefrLevel, onPick: (CefrLevel) -> Unit) {
    val levels = CefrLevel.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        levels.forEachIndexed { i, l ->
            SegmentedButton(
                level == l,
                { onPick(l) },
                SegmentedButtonDefaults.itemShape(i, levels.size),
                icon = {}, // no check mark: it would push the label off the centre of a narrow button
                label = { Text(l.label, maxLines = 1) },
            )
        }
    }
}

/**
 * The editable card rules of the selected level, with a button to go back to the built-in text.
 *
 * @param prefs the current settings.
 * @param settings where changes are saved; every edit is saved at once.
 */
@Composable
private fun RulesEditor(prefs: Prefs, settings: Settings) {
    val level = prefs.level
    val default = remember(level) { Prompts.defaultRules(level) }
    val customised = prefs.customRules.containsKey(level)
    // Local text state avoids cursor jumps while typing.
    var resets by remember { mutableIntStateOf(0) }
    var rules by remember(level, resets) { mutableStateOf(prefs.customRules[level] ?: default) }
    OutlinedTextField(
        rules,
        {
            rules = it
            settings.setCustomRules(level, if (it == default) "" else it)
        },
        Modifier.fillMaxWidth(),
        minLines = 8,
        maxLines = 16,
        label = { Text(rulesFieldLabel(level.label, customised)) },
    )
    Hint(
        "You can use {source_language}, {target_language} and {level}. The JSON reply format is added by the app and " +
            "cannot be changed. Keep the rule that examples must not come from the book, so cards do not quote it.",
    )
    if (customised) {
        OutlinedButton({
            settings.setCustomRules(level, "")
            resets++
        }) { Text("Reset ${level.label} rules to default") }
    }
}

/**
 * The field for instructions that apply to every level.
 *
 * @param saved the saved text.
 * @param onChange called with the new text on every edit.
 */
@Composable
private fun ExtraInstructions(saved: String, onChange: (String) -> Unit) {
    var extra by remember { mutableStateOf(saved) }
    OutlinedTextField(
        extra,
        {
            extra = it
            onChange(it)
        },
        Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 6,
        label = { Text("Extra instructions (added to every level)") },
        placeholder = { Text("e.g. focus on legal vocabulary; use British transcription") },
    )
}
