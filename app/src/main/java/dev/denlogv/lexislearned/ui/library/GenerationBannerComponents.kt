package dev.denlogv.lexislearned.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.ui.AppProgress

/**
 * The banner above the deck list that keeps deck generation visible while the user is elsewhere: progress while it runs,
 * the result when it is done, and the reason when it failed. Tapping it opens the deck or the generation screen.
 *
 * @param banner what to show.
 * @param onOpenDeck called with the deck's id when a finished deck is tapped.
 * @param onOpenProgress called to open the generation screen.
 * @param onStop called when the user stops a running generation.
 * @param onContinue called when the user continues a generation that was stopped.
 * @param onDismiss called when the user dismisses a result.
 */
@Composable
fun GenerationBanner(
    banner: BannerUi,
    onOpenDeck: (Long) -> Unit,
    onOpenProgress: () -> Unit,
    onStop: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(16.dp).clickable { banner.deckId?.let(onOpenDeck) ?: onOpenProgress() }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(banner.title, style = MaterialTheme.typography.titleMedium)
            banner.progress?.let { AppProgress(it, Modifier.fillMaxWidth()) }
            Text(banner.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.align(Alignment.End)) {
                if (banner.running) TextButton(onStop) { Text("Stop") }
                if (banner.resumable) TextButton(onContinue) { Text("Continue") }
                if (!banner.running) TextButton(onDismiss) { Text("Dismiss") }
            }
        }
    }
}
