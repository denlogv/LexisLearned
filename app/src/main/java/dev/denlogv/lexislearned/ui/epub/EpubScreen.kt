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
import dev.denlogv.lexislearned.ui.appViewModel
import dev.denlogv.lexislearned.ui.library.ContentResolverDeckFiles

/**
 * The "deck from EPUB" screen: choose a book, review its structure, generate the deck and follow the progress.
 *
 * @param continuing whether the screen is for adding the rest of a book to a deck that was stopped early.
 * @param onBack called when the back button is pressed.
 * @param onSettings called to open the settings.
 * @param onOpenDeck called with the id of the deck that was created.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubScreen(continuing: Boolean, onBack: () -> Unit, onSettings: () -> Unit, onOpenDeck: (Long) -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { app, _ -> EpubViewModel(app.generation, app.settings, ContentResolverDeckFiles(context), continuing) }
    val prefs by vm.prefs.collectAsState()
    val review by vm.review.collectAsState()
    val reviewed by vm.target.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deck from EPUB") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        bottomBar = {
            reviewed?.let { GenerateBar(review.estimate(it.book), review.selected.size, prefs, vm::generate, onSettings) }
        },
    ) { padding -> EpubBody(vm, onSettings, onOpenDeck, Modifier.padding(padding)) }
}

/**
 * The content of the screen: the review of a book when there is one, otherwise the stage the generation is in.
 *
 * @param vm the screen's view model.
 * @param onSettings called to open the settings.
 * @param onOpenDeck called with the id of the deck that was created.
 * @param modifier layout modifier.
 */
@Composable
private fun EpubBody(vm: EpubViewModel, onSettings: () -> Unit, onOpenDeck: (Long) -> Unit, modifier: Modifier) {
    val state by vm.state.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val review by vm.review.collectAsState()
    val reviewed by vm.target.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.choose(it.toString()) } }
    val current = reviewed
    if (current != null) {
        ReviewList(current.book, review, prefs.level, vm::update, vm::setLevel, current.continuation != null, vm::restart, modifier)
    } else {
        Box(modifier) { StatusPanel(state, prefs, vm, { picker.launch(arrayOf("application/epub+zip", "*/*")) }, onSettings, onOpenDeck) }
    }
}
