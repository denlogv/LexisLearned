package dev.denlogv.lexislearned.ui.epub

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.denlogv.lexislearned.ai.GenState
import dev.denlogv.lexislearned.ui.appViewModel
import dev.denlogv.lexislearned.ui.library.ContentResolverDeckFiles

/**
 * The "deck from EPUB" screen: choose a book, review its structure, generate the deck and follow the progress.
 *
 * @param onBack called when the back button is pressed.
 * @param onSettings called to open the settings.
 * @param onOpenDeck called with the id of the deck that was created.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubScreen(onBack: () -> Unit, onSettings: () -> Unit, onOpenDeck: (Long) -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { app, _ -> EpubViewModel(app.generation, app.settings, ContentResolverDeckFiles(context)) }
    val state by vm.state.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val review by vm.review.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.choose(it.toString()) } }
    val ready = state as? GenState.Ready
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deck from EPUB") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        bottomBar = {
            if (ready != null) GenerateBar(review.estimate(ready.book), review.selected.size, prefs, vm::generate, onSettings)
        },
    ) { padding ->
        if (ready != null) {
            ReviewList(ready.book, review, prefs.level, vm::update, vm::setLevel, Modifier.padding(padding))
        } else {
            Box(Modifier.padding(padding)) {
                StatusPanel(state, prefs, vm, { picker.launch(arrayOf("application/epub+zip", "*/*")) }, onSettings, onOpenDeck)
            }
        }
    }
}
