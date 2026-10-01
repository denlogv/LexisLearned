package dev.denlogv.lexislearned.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.denlogv.lexislearned.format.DeckFormat
import dev.denlogv.lexislearned.ui.appViewModel

/**
 * The library: all decks, with buttons to add one, open settings and manage each deck.
 *
 * @param onOpenDeck called with a deck's id when the user opens it.
 * @param onSettings called when the user opens the settings.
 * @param onEpub called when the user wants to create a deck from an EPUB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpenDeck: (Long) -> Unit, onSettings: () -> Unit, onEpub: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { app, _ -> LibraryViewModel(app.repository, app.settings, ContentResolverDeckFiles(context)) }
    val decks by vm.decks.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pending by remember { mutableStateOf<DeckAction?>(null) }
    var pendingExport by remember { mutableStateOf<Pair<Long, DeckFormat>?>(null) }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.import(it.toString()) } }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        pendingExport?.let { (deckId, format) -> uri?.let { vm.export(it.toString(), deckId, format) } }
        pendingExport = null
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("LexisLearned") }, actions = { SettingsButton(onSettings) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { AddDeckButton(onImport = { importer.launch(arrayOf("*/*")) }, onEpub = onEpub) },
    ) { padding ->
        DeckList(
            decks = decks,
            onOpen = onOpenDeck,
            onExport = { deck, format -> startExport(deck.id, deck.title, format, exporter) { pendingExport = it } },
            onAction = { pending = it },
            modifier = Modifier.padding(padding),
        )
    }
    pending?.let { DeckActionDialog(it, vm, onDismiss = { pending = null }) }
}

/**
 * The button that opens settings.
 *
 * @param onClick called when pressed.
 */
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick) { Icon(Icons.Default.Settings, "Settings") }
}

/**
 * Asks Android for a place to save a deck and remembers what is to be saved there.
 *
 * @param deckId the deck to export.
 * @param title the deck's title, used as the suggested file name.
 * @param format the file format.
 * @param exporter the file picker for creating documents.
 * @param remember stores the pending export until the picker returns.
 */
private fun startExport(
    deckId: Long,
    title: String,
    format: DeckFormat,
    exporter: ActivityResultLauncher<String>,
    remember: (Pair<Long, DeckFormat>) -> Unit,
) {
    remember(deckId to format)
    exporter.launch("$title.${format.extensions.first()}")
}
