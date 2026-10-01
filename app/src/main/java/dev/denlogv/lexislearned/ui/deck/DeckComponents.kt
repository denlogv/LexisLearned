package dev.denlogv.lexislearned.ui.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.ChapterSummary
import dev.denlogv.lexislearned.ui.AppProgress
import dev.denlogv.lexislearned.ui.ConfirmDialog
import dev.denlogv.lexislearned.ui.progressLabel

/**
 * The frame shared by the book, part and chapter screens: a top bar with back button and a reset menu, and a scrolling list.
 *
 * @param title the title in the top bar.
 * @param resetLabel the text of the reset menu entry, for example "Reset book progress".
 * @param onBack called when the back button is pressed.
 * @param onReset called when the user picks the reset entry.
 * @param content adds the items of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScaffold(title: String, resetLabel: String, onBack: () -> Unit, onReset: () -> Unit, content: LazyListScope.() -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title, maxLines = 1) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = { ResetMenu(resetLabel, onReset) },
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp), content = content)
    }
}

/**
 * The block at the top of a detail screen: native title, progress, study options and the study button.
 *
 * @param nativeTitle the title in the native language; hidden if null or equal to [title].
 * @param title the title in the learned language, to compare with.
 * @param totals the progress to show; null hides the progress (while loading).
 * @param options the direction and mode choices.
 * @param studyLabel the text of the study button.
 * @param studyEnabled whether the study button can be pressed.
 * @param onStudy called when the study button is pressed.
 */
@Composable
fun DetailHeader(
    nativeTitle: String?,
    title: String?,
    totals: ProgressTotals?,
    options: @Composable () -> Unit,
    studyLabel: String,
    studyEnabled: Boolean,
    onStudy: () -> Unit,
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        nativeTitle?.takeIf { it != title }?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (totals != null) {
            AppProgress(totals.progress, Modifier.fillMaxWidth())
            Text(
                progressLabel(totals.completed, totals.total, totals.inProgress, totals.progress, totals.due),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        options()
        Button(onStudy, Modifier.fillMaxWidth(), enabled = studyEnabled) { Text(studyLabel) }
    }
}

/**
 * A section heading in a list.
 *
 * @param text the heading.
 */
@Composable
fun ListHeading(text: String) {
    Text(text, Modifier.padding(16.dp, 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

/**
 * A chapter in a list, with its progress bar and progress line.
 *
 * @param chapter the chapter.
 * @param onClick called when the row is tapped.
 */
@Composable
fun ChapterRow(chapter: ChapterSummary, onClick: () -> Unit) {
    ProgressRow(
        chapter.title,
        chapter.nativeTitle,
        chapter.toTotals(),
        null,
        onClick,
    )
}

/**
 * A list row with a title, optional native title and a progress bar; used for parts and chapters.
 *
 * @param title the title.
 * @param nativeTitle the title in the native language, shown if it differs from [title].
 * @param totals the progress to show.
 * @param extra text put in front of the progress line, for example "3 chapters".
 * @param onClick called when the row is tapped.
 */
@Composable
fun ProgressRow(title: String, nativeTitle: String?, totals: ProgressTotals, extra: String?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                nativeTitle?.takeIf { it != title }?.let { Text(it) }
                AppProgress(totals.progress, Modifier.fillMaxWidth())
                Text(
                    progressLabel(totals.completed, totals.total, totals.inProgress, totals.progress, totals.due, extra),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (totals.due > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/**
 * The confirmation dialog for a reset; it closes after either choice.
 *
 * @param title the question, for example "Reset this book?".
 * @param text what the reset does.
 * @param onConfirm called when the user confirms.
 * @param onDismiss called when the dialog closes.
 */
@Composable
fun ResetDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(title, text, "Reset", onConfirm, onDismiss)
}

/**
 * An overflow menu with a single reset entry, used in the top bars.
 *
 * @param label the text of the entry.
 * @param onReset called when the entry is chosen.
 */
@Composable
fun ResetMenu(label: String, onReset: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.Default.MoreVert, "More") }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    open = false
                    onReset()
                },
            )
        }
    }
}
