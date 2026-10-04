package dev.denlogv.lexislearned.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
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
import dev.denlogv.lexislearned.data.DeckSummary
import dev.denlogv.lexislearned.format.DeckFormat
import dev.denlogv.lexislearned.ui.appViewModel

/**
 * The library: all decks, with buttons to add one, open settings and manage each deck.
 *
 * @param onOpenDeck called with a deck's id when the user opens it.
 * @param onSettings called when the user opens the settings.
 * @param onEpub called when the user wants to create a deck from an EPUB.
 * @param onContinueEpub called when the user wants to pick the sections of a paused generation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpenDeck: (Long) -> Unit, onSettings: () -> Unit, onEpub: () -> Unit, onContinueEpub: () -> Unit) {
    val vm = rememberLibraryViewModel()
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
        LibraryBody(
            vm = vm,
            onOpenDeck = onOpenDeck,
            onEpub = onEpub,
            onContinueEpub = onContinueEpub,
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

/**
 * The content under the top bar: the generation banner, when there is something to report, above the deck list.
 *
 * @param vm the library view model.
 * @param onOpenDeck called with a deck's id when it is opened.
 * @param onEpub called to open the generation screen.
 * @param onContinueEpub called to open the review to pick the sections of a paused generation.
 * @param onExport called when the user exports a deck in a format.
 * @param onAction called when the user asks for an action that needs confirmation.
 * @param modifier layout modifier.
 */
@Composable
private fun LibraryBody(
    vm: LibraryViewModel,
    onOpenDeck: (Long) -> Unit,
    onEpub: () -> Unit,
    onContinueEpub: () -> Unit,
    onExport: (DeckSummary, DeckFormat) -> Unit,
    onAction: (DeckAction) -> Unit,
    modifier: Modifier,
) {
    val decks by vm.decks.collectAsState()
    val generating by vm.generating.collectAsState()
    Column(modifier) {
        generating.toBanner()?.let {
            val actions = BannerActions(
                onOpenDeck,
                onEpub,
                vm::pauseGenerationAfterSection,
                vm::pauseGenerationNow,
                vm::resumeGeneration,
                onContinueEpub,
                vm::discardGeneration,
                vm::dismissGeneration,
            )
            GenerationBanner(it, actions)
        }
        DeckList(decks, onOpenDeck, onExport, onAction, Modifier.weight(1f))
    }
}

/**
 * Creates the library's view model with what it needs from the application.
 *
 * @return the view model, kept across recompositions and configuration changes.
 */
@Composable
private fun rememberLibraryViewModel(): LibraryViewModel {
    val context = LocalContext.current
    return appViewModel { app, _ ->
        LibraryViewModel(app.library, app.storage, app.settings, ContentResolverDeckFiles(context), app.generation)
    }
}
