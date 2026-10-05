package dev.denlogv.lexislearned.ui

import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.denlogv.lexislearned.LexisLearnedApp

/**
 * The application object, which holds the settings, repository and other long-lived objects.
 *
 * @return the app.
 */
@Composable
fun app(): LexisLearnedApp = LocalContext.current.applicationContext as LexisLearnedApp

/**
 * Creates a view model (of type `VM`) that can reach the application's objects and the navigation arguments.
 *
 * @param create builds the view model from the application and the saved state (which holds the navigation arguments).
 * @return the view model, kept across recompositions and configuration changes.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (LexisLearnedApp, SavedStateHandle) -> VM): VM {
    val application = app()
    return viewModel(
        factory = viewModelFactory {
            initializer { create(application, createSavedStateHandle()) }
        },
    )
}

/**
 * Makes a card react to taps, with the press highlight clipped to the card's rounded shape. A plain clickable on a card's modifier
 * draws a square highlight whose corners stick out of the card.
 *
 * @param enabled whether taps are accepted.
 * @param onClick called when the card is tapped.
 * @return this modifier with the clip and the click added.
 */
@Composable
fun Modifier.cardClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clip(CardDefaults.shape).clickable(enabled = enabled, onClick = onClick)

/**
 * A progress bar without the trailing stop dot and the gap Material 3 draws by default.
 *
 * @param progress progress from 0 to 1; values outside are clamped.
 * @param modifier layout modifier.
 */
@Composable
fun AppProgress(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/**
 * A confirmation dialog with a confirm and a cancel button; it closes after either.
 *
 * @param title the dialog title.
 * @param text the explanation.
 * @param confirmLabel the label of the confirm button.
 * @param onConfirm called when the user confirms.
 * @param onDismiss called when the dialog closes, after a confirmation as well.
 */
@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton({
                onConfirm()
                onDismiss()
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
