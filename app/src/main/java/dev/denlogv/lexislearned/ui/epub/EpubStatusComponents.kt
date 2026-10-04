package dev.denlogv.lexislearned.ui.epub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.ui.AppProgress
import dev.denlogv.lexislearned.ui.PAUSE_AFTER_SECTION_LABEL
import dev.denlogv.lexislearned.ui.PAUSE_NOW_LABEL
import dev.denlogv.lexislearned.ui.headline
import dev.denlogv.lexislearned.ui.pauseAfterSectionLabel
import dev.denlogv.lexislearned.ui.plural
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
            is GenState.Running -> RunningPanel(state, PauseActions(vm::pauseAfterSection, vm::pauseNow), onOpenDeck)
            is GenState.Paused -> PausedNotice(state)
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
 * @param pause what the two pause buttons do.
 * @param onOpenDeck called with the id of the deck as far as it is generated.
 */
@Composable
private fun RunningPanel(state: GenState.Running, pause: PauseActions, onOpenDeck: (Long) -> Unit) {
    AppProgress(if (state.total == 0) 0f else state.done / state.total.toFloat(), Modifier.fillMaxWidth())
    Text(state.summary())
    Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        "Chapters appear in your library as soon as they are ready. You can leave this screen; progress is shown in the library. " +
            "“$PAUSE_AFTER_SECTION_LABEL” waits for the section in progress, or for the first one if none has started yet; " +
            "that request is paid for already. “$PAUSE_NOW_LABEL” gives the request in flight up, and the section is done again on resume.",
        style = MaterialTheme.typography.bodySmall,
    )
    state.deckId?.let { Button({ onOpenDeck(it) }) { Text("Open deck so far") } }
    PauseButtons(state, pause)
}

/**
 * The pause buttons of a running generation: the one that waits for the section is plain text, the one that stops at once also has the
 * pause icon. They wrap onto a second line on a narrow screen.
 *
 * @param state the progress, which tells whether the pause after the section was asked for already.
 * @param pause what the buttons do.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PauseButtons(state: GenState.Running, pause: PauseActions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.pauseAfterSectionLabel()?.let { OutlinedButton(pause.afterSection) { Text(it) } }
        IconTextButton(Icons.Default.Pause, PAUSE_NOW_LABEL, pause.now)
    }
}

/**
 * An outlined button with an icon in front of its label.
 *
 * @param icon the icon.
 * @param label the text.
 * @param onClick called when the button is pressed.
 */
@Composable
private fun IconTextButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick) {
        Icon(icon, null, Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(label)
    }
}

/**
 * What the pause buttons of a running generation do.
 *
 * @property afterSection pauses once the section in progress is done.
 * @property now pauses at once, giving up the request in flight.
 */
class PauseActions(val afterSection: () -> Unit, val now: () -> Unit)

/**
 * What the screen says while a generation is paused, stopped by a problem or was interrupted by the app closing: only one book can be
 * generated at a time, and the library's banner is where the paused one is resumed, narrowed down or discarded.
 *
 * @param state the paused generation.
 */
@Composable
private fun PausedNotice(state: GenState.Paused) {
    Text(state.headline(), style = MaterialTheme.typography.titleMedium)
    Text("“${state.book.title}” · ${state.summary()}")
    Text(
        "Resume it, choose its sections or discard the rest from the banner above the library, then come back to create another deck.",
        style = MaterialTheme.typography.bodySmall,
    )
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
