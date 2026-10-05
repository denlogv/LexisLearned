package dev.denlogv.lexislearned.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.ui.AppProgress
import dev.denlogv.lexislearned.ui.ConfirmDialog
import dev.denlogv.lexislearned.ui.cardClickable

/**
 * The banner above the deck list that keeps deck generation visible while the user is elsewhere: progress while it runs,
 * the result when it is done, and the reason when it failed. Every banner has an info button that explains it. A paused generation is
 * handled right here: resume it, choose its sections or discard the rest with the cross in the corner, after a warning. Only a finished
 * deck leads anywhere: tapping the banner opens it.
 *
 * @param banner what to show.
 * @param actions what the banner's buttons and taps do.
 */
@Composable
fun GenerationBanner(banner: BannerUi, actions: BannerActions) {
    var confirming by remember { mutableStateOf(false) }
    var explaining by remember { mutableStateOf(false) }
    val open = banner.deckId?.let { id -> Modifier.cardClickable { actions.onOpenDeck(id) } } ?: Modifier
    val onClose = if (banner.discardWarning != null) ({ confirming = true }) else actions.onDismiss // a paused run asks first
    Card(Modifier.fillMaxWidth().padding(16.dp).then(open)) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BannerHeader(banner, onClose) { explaining = true }
            banner.progress?.let { AppProgress(it, Modifier.fillMaxWidth().padding(end = 8.dp)) }
            Text(banner.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BannerButtons(banner, actions)
        }
    }
    BannerDialogs(banner, actions.onDiscard, confirming, explaining) {
        confirming = false
        explaining = false
    }
}

/**
 * The title of the banner with the info button and, when there is something to close, the cross in the top right corner.
 *
 * @param banner what the banner shows.
 * @param onClose called when the cross is pressed.
 * @param onInfo called when the info button is pressed.
 */
@Composable
private fun BannerHeader(banner: BannerUi, onClose: () -> Unit, onInfo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(banner.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        LabelledIconButton(Icons.Default.Info, INFO_LABEL, onInfo)
        if (banner.closable) LabelledIconButton(Icons.Default.Close, banner.closeLabel, onClose)
    }
}

/**
 * The dialogs of the banner: the explanation behind the info button and the warning before the rest of a paused generation is discarded.
 *
 * @param banner what the banner shows.
 * @param onDiscard called when the user confirmed the discarding.
 * @param confirming whether the discard warning is open.
 * @param explaining whether the explanation is open.
 * @param onClose called when a dialog closes.
 */
@Composable
private fun BannerDialogs(banner: BannerUi, onDiscard: () -> Unit, confirming: Boolean, explaining: Boolean, onClose: () -> Unit) {
    if (explaining) {
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text(banner.title) },
            text = { Text(banner.info) },
            confirmButton = { TextButton(onClose) { Text("OK") } },
        )
    }
    if (confirming) banner.discardWarning?.let { ConfirmDialog("Discard the rest?", it, "Discard", onDiscard, onClose) }
}

/**
 * The buttons of the banner. They wrap onto a second line on a narrow screen instead of squeezing each other. Pausing after the section
 * is written out, because an icon for it was not understood; the rest are icons.
 *
 * @param banner what the banner offers.
 * @param actions what the buttons do.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BannerButtons(banner: BannerUi, actions: BannerActions) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.Center) {
        banner.pauseAfterSectionLabel?.let { TextButton(actions.onPauseAfterSection) { Text(it) } }
        banner.pauseNowLabel?.let { LabelledIconButton(Icons.Default.Pause, it, actions.onPauseNow) }
        banner.resumeLabel?.let {
            LabelledIconButton(Icons.Default.Checklist, CHOOSE_SECTIONS_LABEL, actions.onChooseSections)
            LabelledIconButton(Icons.Default.PlayArrow, it, actions.onResume)
        }
    }
}

private const val CHOOSE_SECTIONS_LABEL = "Choose sections"
private const val INFO_LABEL = "What is this?"

/**
 * A button that shows only an icon; the label is what screen readers say.
 *
 * @param icon the icon.
 * @param label what the button does, as the description of the icon.
 * @param onClick called when the button is pressed.
 */
@Composable
private fun LabelledIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick) { Icon(icon, label) }
}
