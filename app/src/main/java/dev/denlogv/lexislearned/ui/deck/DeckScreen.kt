package dev.denlogv.lexislearned.ui.deck

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.denlogv.lexislearned.ui.Routes
import dev.denlogv.lexislearned.ui.appViewModel
import dev.denlogv.lexislearned.ui.plural
import dev.denlogv.lexislearned.ui.studyButtonLabel

/**
 * The book screen: overall progress, study options, and the book's parts and chapters.
 *
 * @param onBack called when the back button is pressed.
 * @param onStudy called with (deck, chapter, part) ids to start a session; [Routes.NONE] stands for "all".
 * @param onChapter called with a chapter's id when the user opens it.
 * @param onPart called with a part's id when the user opens it.
 */
@Composable
fun DeckScreen(onBack: () -> Unit, onStudy: (Long, Long, Long) -> Unit, onChapter: (Long) -> Unit, onPart: (Long) -> Unit) {
    val vm = appViewModel { app, handle -> DeckViewModel(app.repository, app.settings, handle["deckId"]!!) }
    val deck by vm.deck.collectAsState()
    val chapters by vm.chapters.collectAsState()
    val entries by vm.entries.collectAsState()
    val prefs by vm.prefs.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    val totals = chapters.totals()
    DetailScaffold(deck?.title.orEmpty(), "Reset book progress", onBack, onReset = { confirmReset = true }) {
        item {
            DetailHeader(
                deck?.nativeTitle,
                deck?.title,
                totals,
                options = { StudyOptions(deck, prefs, vm::setDirection, vm::toggleMode) },
                studyLabel = studyButtonLabel("book", totals.due),
                studyEnabled = true,
                onStudy = { onStudy(vm.deckId, Routes.NONE, Routes.NONE) },
            )
        }
        item { ListHeading(if (entries.any { it is DeckEntry.PartRow }) "Contents" else "Chapters") }
        items(entries, key = { it.key }) { entry -> EntryRow(entry, onChapter, onPart) }
    }
    if (confirmReset) {
        ResetDialog("Reset this book?", "All study progress in the whole book is cleared: every word becomes new again.", vm::resetBook) {
            confirmReset = false
        }
    }
}

/**
 * One row of the book screen.
 *
 * @param entry a part or a chapter outside any part.
 * @param onChapter called with a chapter's id when tapped.
 * @param onPart called with a part's id when tapped.
 */
@Composable
private fun EntryRow(entry: DeckEntry, onChapter: (Long) -> Unit, onPart: (Long) -> Unit) {
    when (entry) {
        is DeckEntry.PartRow -> ProgressRow(
            entry.part.title,
            entry.part.nativeTitle,
            entry.totals,
            plural(entry.chapters, "chapter"),
        ) { onPart(entry.part.id) }
        is DeckEntry.ChapterRow -> ChapterRow(entry.chapter) { onChapter(entry.chapter.id) }
    }
}
