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
import dev.denlogv.lexislearned.ui.studyButtonLabel

/**
 * The part screen (a novel in an omnibus, a part of a book): its progress, study options and chapters.
 *
 * @param onBack called when the back button is pressed.
 * @param onStudy called with (deck, chapter, part) ids to start a session; [Routes.NONE] stands for "all".
 * @param onChapter called with a chapter's id when the user opens it.
 */
@Composable
fun PartScreen(onBack: () -> Unit, onStudy: (Long, Long, Long) -> Unit, onChapter: (Long) -> Unit) {
    val vm = appViewModel { app, handle -> PartViewModel(app.library, app.settings, handle["partId"]!!) }
    val part by vm.part.collectAsState()
    val deck by vm.deck.collectAsState()
    val chapters by vm.chapters.collectAsState()
    val prefs by vm.prefs.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    val totals = chapters.totals()
    DetailScaffold(part?.title.orEmpty(), "Reset part progress", onBack, onReset = { confirmReset = true }) {
        item {
            DetailHeader(
                part?.nativeTitle,
                part?.title,
                totals,
                options = { StudyOptions(deck, prefs, vm::setDirection, vm::toggleMode) },
                studyLabel = studyButtonLabel("part", totals.due),
                studyEnabled = deck != null,
                onStudy = { deck?.let { onStudy(it.id, Routes.NONE, vm.partId) } },
            )
        }
        item { ListHeading("Chapters") }
        items(chapters, key = { it.id }) { chapter -> ChapterRow(chapter) { onChapter(chapter.id) } }
    }
    if (confirmReset) {
        ResetDialog("Reset this part?", "Study progress of all chapters in this part is cleared.", vm::resetPart) { confirmReset = false }
    }
}
