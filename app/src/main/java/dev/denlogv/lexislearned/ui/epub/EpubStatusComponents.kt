package dev.denlogv.lexislearned.ui.epub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.ui.AppProgress
import dev.denlogv.lexislearned.ui.plural

/**
 * What the screen shows when there is no book to review: the start, reading, running, finished and failed stages.
 *
 * @param state the stage; [GenState.Ready] is shown elsewhere.
 * @param prefs the current settings.
 * @param vm the screen's view model.
 * @param onChoose called when the user wants to pick an EPUB.
 * @param onSettings called to open the settings.
 * @param onOpenDeck called with the id of the finished deck.
 */
@Composable
fun StatusPanel(
    state: GenState,
    prefs: Prefs,
    vm: EpubViewModel,
    onChoose: () -> Unit,
    onSettings: () -> Unit,
    onOpenDeck: (Long) -> Unit,
) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            GenState.Idle -> IdlePanel(prefs, onChoose)
            GenState.Loading -> CircularProgressIndicator()
            is GenState.Running -> RunningPanel(state, vm::cancel)
            is GenState.Finished -> FinishedPanel(state, onOpenDeck)
            is GenState.Failed -> FailedPanel(state, vm::restart, onSettings)
            is GenState.Ready -> Unit
        }
    }
}

/**
 * The start: what the feature does and a button to choose a book.
 *
 * @param prefs the current settings, for the language and level named in the text.
 * @param onChoose called when the button is tapped.
 */
@Composable
private fun IdlePanel(prefs: Prefs, onChoose: () -> Unit) {
    Text(
        "Pick an EPUB book. LexisLearned reads its structure (parts and chapters), lets you review it, " +
            "then asks an AI model (with your API key) for vocabulary above ${prefs.level.label} — translated into " +
            "${prefs.targetLang.uppercase()}, with transcription and original example sentences.",
    )
    Button(onChoose) { Text("Choose EPUB…") }
}

/**
 * Progress while cards are generated.
 *
 * @param state the progress.
 * @param onStop called when the user stops early.
 */
@Composable
private fun RunningPanel(state: GenState.Running, onStop: () -> Unit) {
    AppProgress(if (state.total == 0) 0f else state.done / state.total.toFloat(), Modifier.fillMaxWidth())
    Text("${state.done} / ${plural(state.total, "section")} · ${plural(state.cards, "card")} so far")
    Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Keep the app open. You can leave this screen; generation continues.", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onStop) { Text("Stop and keep what's done") }
}

/**
 * The result of a finished generation.
 *
 * @param state the result.
 * @param onOpenDeck called with the new deck's id.
 */
@Composable
private fun FinishedPanel(state: GenState.Finished, onOpenDeck: (Long) -> Unit) {
    Text("Created a deck with ${plural(state.cards, "card")}.", style = MaterialTheme.typography.titleMedium)
    if (state.failed.isNotEmpty()) {
        Text("${state.failed.size} section(s) failed and were skipped:", color = MaterialTheme.colorScheme.error)
        state.failed.take(MAX_LISTED_FAILURES).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
    }
    Button({ onOpenDeck(state.deckId) }) { Text("Open deck") }
}

/** How many failed sections are listed by name. */
private const val MAX_LISTED_FAILURES = 5

/**
 * An error with ways to recover.
 *
 * @param state the error.
 * @param onRetry called to go back to choosing a file.
 * @param onSettings called to open the settings.
 */
@Composable
private fun FailedPanel(state: GenState.Failed, onRetry: () -> Unit, onSettings: () -> Unit) {
    Text(state.message, color = MaterialTheme.colorScheme.error)
    Button(onRetry) { Text("Try again") }
    OutlinedButton(onSettings) { Text("Open settings") }
}

/**
 * The bar under the review: the size estimate, a hint about the API key and the "Generate" button.
 *
 * @param estimate the expected size of the request.
 * @param sections number of selected sections.
 * @param prefs the current settings.
 * @param onGenerate called when the button is tapped.
 * @param onSettings called to open the settings.
 */
@Composable
fun GenerateBar(estimate: Estimate, sections: Int, prefs: Prefs, onGenerate: () -> Unit, onSettings: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        // The bar sits at the screen edge, so it keeps its content clear of the system navigation bar itself.
        Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${plural(sections, "section")} · ~${plural(estimate.cards, "card")} · ~${estimate.tokensK}k input tokens " +
                    "(billed by ${prefs.provider.label} to your key)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!prefs.ready) {
                Text(
                    "The AI provider is not fully set up.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onSettings, Modifier.fillMaxWidth()) { Text("Open settings") }
            }
            Button(onGenerate, Modifier.fillMaxWidth(), enabled = prefs.ready && sections > 0) { Text("Generate deck") }
        }
    }
}
