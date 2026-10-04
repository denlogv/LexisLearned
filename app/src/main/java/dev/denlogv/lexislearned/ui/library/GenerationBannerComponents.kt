package dev.denlogv.lexislearned.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.ui.AppProgress
import dev.denlogv.lexislearned.ui.ConfirmDialog

/**
 * The banner above the deck list that keeps deck generation visible while the user is elsewhere: progress while it runs,
 * the result when it is done, and the reason when it failed. Tapping it opens the deck or the generation screen. A paused generation
 * is handled right here: resume, choose its sections or discard the rest after a warning.
 *
 * @param banner what to show.
 * @param actions what the banner's buttons and taps do.
 */
@Composable
fun GenerationBanner(banner: BannerUi, actions: BannerActions) {
    var confirming by remember { mutableStateOf(false) }
    val tap = Modifier.clickable(enabled = banner.tappable) { banner.deckId?.let(actions.onOpenDeck) ?: actions.onOpenProgress() }
    Card(Modifier.fillMaxWidth().padding(16.dp).then(tap)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(banner.title, style = MaterialTheme.typography.titleMedium)
            banner.progress?.let { AppProgress(it, Modifier.fillMaxWidth()) }
            Text(banner.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BannerButtons(banner, actions) { confirming = true }
        }
    }
    if (confirming) {
        banner.discardWarning?.let {
            ConfirmDialog("Discard the rest?", it, "Discard", actions.onDiscard) { confirming = false }
        }
    }
}

/**
 * The buttons of the banner. They wrap onto a second line on a narrow screen instead of squeezing each other.
 *
 * @param banner what the banner offers.
 * @param actions what the buttons do.
 * @param onAskDiscard called when the user wants to discard the rest, to show the warning first.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BannerButtons(banner: BannerUi, actions: BannerActions, onAskDiscard: () -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.Center) {
        banner.pauseAfterSectionLabel?.let { TextButton(actions.onPauseAfterSection) { Text(it) } }
        banner.pauseNowLabel?.let { LabelledIconButton(Icons.Default.Pause, it, actions.onPauseNow) }
        if (banner.discardWarning != null) {
            LabelledIconButton(Icons.Default.Delete, DISCARD_LABEL, onAskDiscard, MaterialTheme.colorScheme.error)
        }
        banner.resumeLabel?.let {
            LabelledIconButton(Icons.Default.Checklist, CHOOSE_SECTIONS_LABEL, actions.onChooseSections)
            LabelledIconButton(Icons.Default.PlayArrow, it, actions.onResume)
        }
        if (banner.dismissible) TextButton(actions.onDismiss) { Text("Dismiss") }
    }
}

private const val DISCARD_LABEL = "Discard the rest"
private const val CHOOSE_SECTIONS_LABEL = "Choose sections"

/**
 * A button that shows only an icon; the label is what screen readers say.
 *
 * @param icon the icon.
 * @param label what the button does, as the description of the icon.
 * @param onClick called when the button is pressed.
 * @param tint the colour of the icon.
 */
@Composable
private fun LabelledIconButton(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = LocalContentColor.current) {
    IconButton(onClick) { Icon(icon, label, tint = tint) }
}
