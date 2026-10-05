package dev.denlogv.lexislearned.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.DeckSummary
import dev.denlogv.lexislearned.format.DeckFormat
import dev.denlogv.lexislearned.format.NativeFormat
import dev.denlogv.lexislearned.ui.AppProgress
import dev.denlogv.lexislearned.ui.ConfirmDialog
import dev.denlogv.lexislearned.ui.cardClickable
import dev.denlogv.lexislearned.ui.progressLabel

/**
 * The floating "Add deck" button with its menu.
 *
 * @param onImport called when the user chooses to import a file.
 * @param onEpub called when the user chooses to create a deck from an EPUB.
 */
@Composable
fun AddDeckButton(onImport: () -> Unit, onEpub: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ExtendedFloatingActionButton(onClick = { open = true }) {
            Icon(Icons.Default.Add, null)
            Text("Add deck", Modifier.padding(start = 8.dp))
        }
        DropdownMenu(open, { open = false }) {
            MenuItem("Import file (.lexis)") {
                open = false
                onImport()
            }
            MenuItem("Create from EPUB (AI)") {
                open = false
                onEpub()
            }
        }
    }
}

/**
 * A menu entry.
 *
 * @param label the text.
 * @param onClick called when chosen.
 */
@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = onClick)
}

/**
 * The list of decks, or a hint when there are none.
 *
 * @param decks the decks; null while loading.
 * @param onOpen called with a deck's id when it is opened.
 * @param onExport called when the user exports a deck in a format.
 * @param onAction called when the user asks for an action that needs confirmation.
 * @param modifier layout modifier.
 */
@Composable
fun DeckList(
    decks: List<DeckSummary>?,
    onOpen: (Long) -> Unit,
    onExport: (DeckSummary, DeckFormat) -> Unit,
    onAction: (DeckAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        decks == null -> Unit
        decks.isEmpty() -> EmptyLibrary(modifier)
        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(decks, key = { it.id }) { deck ->
                DeckCard(
                    deck,
                    onClick = { onOpen(deck.id) },
                    onExport = { onExport(deck, it) },
                    onReset = { onAction(DeckAction.Reset(deck)) },
                    onDelete = { onAction(DeckAction.Delete(deck)) },
                )
            }
        }
    }
}

/**
 * The hint shown when the library is empty.
 *
 * @param modifier layout modifier.
 */
@Composable
private fun EmptyLibrary(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No decks yet", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Import a LexisLearned (.lexis) file, or generate a vocabulary deck from an EPUB book.",
            Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One deck in the list.
 *
 * @param deck the deck and its progress.
 * @param onClick called when the card is tapped.
 * @param onExport called when an export format is chosen from the menu.
 * @param onReset called when "Reset progress" is chosen.
 * @param onDelete called when "Delete" is chosen.
 */
@Composable
private fun DeckCard(deck: DeckSummary, onClick: () -> Unit, onExport: (DeckFormat) -> Unit, onReset: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().cardClickable(onClick = onClick)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            DeckCardText(deck, Modifier.weight(1f))
            DeckCardMenu(onExport, onReset, onDelete)
        }
    }
}

/**
 * The titles, progress bar and progress line of a deck.
 *
 * @param deck the deck and its progress.
 * @param modifier layout modifier.
 */
@Composable
private fun DeckCardText(deck: DeckSummary, modifier: Modifier) {
    Column(modifier) {
        Text(deck.title, style = MaterialTheme.typography.titleMedium)
        deck.nativeTitle?.takeIf { it != deck.title }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppProgress(deck.progress, Modifier.fillMaxWidth().padding(top = 12.dp, end = 12.dp))
        Text(
            progressLabel(deck.completed, deck.total, deck.inProgress, deck.progress, deck.due),
            Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (deck.due > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The "more" menu of a deck card.
 *
 * @param onExport called when an export format is chosen.
 * @param onReset called when "Reset progress" is chosen.
 * @param onDelete called when "Delete" is chosen.
 */
@Composable
private fun DeckCardMenu(onExport: (DeckFormat) -> Unit, onReset: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.Default.MoreVert, "More") }
        DropdownMenu(open, { open = false }) {
            val choose = { action: () -> Unit ->
                open = false
                action()
            }
            MenuItem("Export as .lexis (with progress)") { choose { onExport(NativeFormat) } }
            MenuItem("Reset progress") { choose(onReset) }
            MenuItem("Delete") { choose(onDelete) }
        }
    }
}

/**
 * The confirmation dialog for a reset or delete.
 *
 * @param action what the user asked for.
 * @param vm performs the action once confirmed.
 * @param onDismiss called when the dialog closes.
 */
@Composable
fun DeckActionDialog(action: DeckAction, vm: LibraryViewModel, onDismiss: () -> Unit) {
    when (action) {
        is DeckAction.Reset -> ConfirmDialog(
            "Reset “${action.deck.title}”?",
            "All study progress in this book is cleared: every word becomes new again. The words themselves stay.",
            "Reset",
            { vm.reset(action.deck.id) },
            onDismiss,
        )
        is DeckAction.Delete -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete “${action.deck.title}”?") },
            text = { Text("The deck and its study progress will be removed from this device.") },
            confirmButton = {
                TextButton({
                    vm.delete(action.deck.id)
                    onDismiss()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
        )
    }
}
