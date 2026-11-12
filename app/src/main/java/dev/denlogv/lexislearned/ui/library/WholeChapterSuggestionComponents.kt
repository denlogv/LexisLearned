package dev.denlogv.lexislearned.ui.library

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.denlogv.lexislearned.ui.appViewModel

/** The one-time dialog that offers whole-chapter sessions; it draws nothing unless the view model says it is due. */
@Composable
fun WholeChapterSuggestion() {
    val vm = appViewModel { app, _ -> WholeChapterSuggestionViewModel(app.settings) }
    val visible by vm.visible.collectAsState()
    if (visible) {
        AlertDialog(
            onDismissRequest = vm::keepNumbers,
            title = { Text("Study a whole chapter at a time?") },
            text = {
                Text(
                    "A session can now take every word of one chapter instead of a fixed number of cards. Your card counts " +
                        "stay saved and apply whenever it is off. You can change this any time in Settings, Study sessions.",
                )
            },
            confirmButton = { Button(vm::useWholeChapters) { Text("Use whole chapters") } },
            dismissButton = { TextButton(vm::keepNumbers) { Text("Keep my numbers") } },
        )
    }
}
