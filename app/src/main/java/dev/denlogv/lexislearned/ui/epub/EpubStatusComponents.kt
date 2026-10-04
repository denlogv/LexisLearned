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
import dev.denlogv.lexislearned.ui.plural

/**
 * What the screen shows when there is no book to review: the start, the reading of a book, and a note while a generation is going on.
 * The progress, a pause, the result and a failure are shown on the banner of the library, not here.
 *
 * @param state the stage; [GenState.Ready] is shown elsewhere.
 * @param prefs the current settings.
 * @param onChoose called when the user wants to pick an EPUB.
 */
@Composable
fun StatusPanel(state: GenState, prefs: Prefs, onChoose: () -> Unit) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            GenState.Idle -> IdlePanel(prefs, onChoose)
            GenState.Loading -> CircularProgressIndicator()
            is GenState.Running, is GenState.Paused -> RunNotice()
            is GenState.Finished, is GenState.Failed, is GenState.Ready -> Unit // The library's banner has them; the screen is closing.
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
 * What the screen says while a generation is running or paused: only one book can be generated at a time, and the banner above the
 * library is where it is followed and handled.
 */
@Composable
private fun RunNotice() {
    Text("A deck is being generated.", style = MaterialTheme.typography.titleMedium)
    Text(
        "Follow it, pause it, resume it or discard the rest on the banner above the library; then come back to create another deck.",
        style = MaterialTheme.typography.bodySmall,
    )
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
