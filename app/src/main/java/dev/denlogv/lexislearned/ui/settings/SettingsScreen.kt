package dev.denlogv.lexislearned.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.denlogv.lexislearned.ui.app
import dev.denlogv.lexislearned.ui.appViewModel

/**
 * The settings overview: one row per page of settings, each with a summary of what is chosen there.
 *
 * @param onBack called when the back button is pressed.
 * @param onStudy called to open the study session settings.
 * @param onGeneration called to open the settings of deck generation from EPUBs.
 * @param onPrompt called to open the level and card prompt settings.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onStudy: () -> Unit, onGeneration: () -> Unit, onPrompt: () -> Unit) {
    val prefs by app().settings.prefs.collectAsState()
    SettingsPage("Settings", onBack) {
        SettingsEntry(Icons.Filled.School, "Study sessions", studySummary(prefs), onStudy)
        SettingsEntry(Icons.Filled.AutoAwesome, "Deck generation", generationSummary(prefs), onGeneration)
        SettingsEntry(Icons.Filled.EditNote, "Level and card prompt", promptSummary(prefs), onPrompt)
    }
}

/**
 * The page with the settings that shape a study session.
 *
 * @param onBack called when the back button is pressed.
 */
@Composable
fun StudySettingsScreen(onBack: () -> Unit) {
    val settings = app().settings
    val prefs by settings.prefs.collectAsState()
    SettingsPage("Study sessions", onBack) { StudySettings(prefs, settings) }
}

/**
 * The page with the settings for creating decks from EPUBs: provider, key, model and the user's language.
 *
 * @param onBack called when the back button is pressed.
 */
@Composable
fun GenerationSettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { app, _ -> SettingsViewModel(app.settings) }
    val prefs by vm.prefs.collectAsState()
    SettingsPage("Deck generation", onBack) { GenerationSettings(prefs, vm) }
}

/**
 * The page with the level, the card rules of that level and extra instructions for deck generation.
 *
 * @param onBack called when the back button is pressed.
 */
@Composable
fun PromptSettingsScreen(onBack: () -> Unit) {
    val settings = app().settings
    val prefs by settings.prefs.collectAsState()
    SettingsPage("Level and card prompt", onBack) { PromptSettings(prefs, settings) }
}
