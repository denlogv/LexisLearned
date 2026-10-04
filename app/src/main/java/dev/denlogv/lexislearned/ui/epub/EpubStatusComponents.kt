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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.ui.AppProgress
import dev.denlogv.lexislearned.ui.headline
import dev.denlogv.lexislearned.ui.pauseLabel
import dev.denlogv.lexislearned.ui.plural
import dev.denlogv.lexislearned.ui.resumeLabel
import dev.denlogv.lexislearned.ui.summary

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
            is GenState.Running -> RunningPanel(state, vm::pause, onOpenDeck)
            is GenState.Paused -> PausedPanel(state, PausedActions(vm::resume, vm::chooseSections, vm::discard, onSettings, onOpenDeck))
            is GenState.Finished -> FinishedPanel(state, onOpenDeck)
            is GenState.Failed -> FailedPanel(state, vm::backToReview, vm::restart, onSettings)
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
 * Progress while cards are generated. The deck is already in the library once its first chapter is done, and grows from there.
 *
 * @param state the progress.
 * @param onPause called when the user pauses.
 * @param onOpenDeck called with the id of the deck as far as it is generated.
 */
@Composable
private fun RunningPanel(state: GenState.Running, onPause: () -> Unit, onOpenDeck: (Long) -> Unit) {
    AppProgress(if (state.total == 0) 0f else state.done / state.total.toFloat(), Modifier.fillMaxWidth())
    Text(state.summary())
    Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        "Chapters appear in your library as soon as they are ready. You can leave this screen or switch to another app: generation " +
            "goes on in the background and shows its progress in a notification. " +
            "Pausing waits for the section in progress, which is paid for already.",
        style = MaterialTheme.typography.bodySmall,
    )
    state.deckId?.let { Button({ onOpenDeck(it) }) { Text("Open deck so far") } }
    OutlinedButton(onPause) { Text(state.pauseLabel()) }
}

/**
 * What the user can do with a paused generation.
 *
 * @property onResume does the sections that are left.
 * @property onChooseSections opens the review to pick the sections.
 * @property onDiscard gives up on the sections that are left.
 * @property onSettings opens the settings.
 * @property onOpenDeck opens the deck with the id given.
 */
class PausedActions(
    val onResume: () -> Unit,
    val onChooseSections: () -> Unit,
    val onDiscard: () -> Unit,
    val onSettings: () -> Unit,
    val onOpenDeck: (Long) -> Unit,
)

/**
 * A generation that is paused, stopped by a problem or was interrupted by the app closing: what is done, what is left and how to go on.
 * It stays like this, also after the app is restarted, until the user resumes or discards it.
 *
 * @param state the paused generation.
 * @param actions what the buttons do.
 */
@Composable
private fun PausedPanel(state: GenState.Paused, actions: PausedActions) {
    Text(state.headline(), style = MaterialTheme.typography.titleMedium)
    Text("“${state.book.title}” · ${state.summary()}")
    state.failed.take(MAX_LISTED_FAILURES).forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
    Text(
        "The chapters that are done are in your library and are not generated or paid for again.",
        style = MaterialTheme.typography.bodySmall,
    )
    Button(actions.onResume) { Text(state.resumeLabel()) }
    state.deckId?.let { OutlinedButton({ actions.onOpenDeck(it) }) { Text("Open deck so far") } }
    OutlinedButton(actions.onChooseSections) { Text("Choose sections…") }
    if (state.failed.isNotEmpty()) OutlinedButton(actions.onSettings) { Text("Open settings") }
    TextButton(actions.onDiscard) { Text("Discard the rest") }
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
    Button({ onOpenDeck(state.deckId) }) { Text("Open deck") }
}

/** How many failed sections are listed by name. */
private const val MAX_LISTED_FAILURES = 5

/**
 * An error with ways to recover.
 *
 * @param state the error.
 * @param onBackToReview called to go back to the review of the book, when there is one.
 * @param onChooseAnother called to go back to choosing a file.
 * @param onSettings called to open the settings.
 */
@Composable
private fun FailedPanel(state: GenState.Failed, onBackToReview: () -> Unit, onChooseAnother: () -> Unit, onSettings: () -> Unit) {
    Text(state.message, color = MaterialTheme.colorScheme.error)
    if (state.book != null) Button(onBackToReview) { Text("Back to the book") }
    OutlinedButton(onChooseAnother) { Text(if (state.book != null) "Choose another book" else "Try again") }
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
