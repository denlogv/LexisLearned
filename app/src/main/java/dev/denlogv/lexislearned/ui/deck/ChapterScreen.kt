package dev.denlogv.lexislearned.ui.deck

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.ChapterEntity
import dev.denlogv.lexislearned.data.DeckEntity
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.ui.Routes
import dev.denlogv.lexislearned.ui.appViewModel
import dev.denlogv.lexislearned.ui.studyButtonLabel

/**
 * The chapter screen: its progress, study options, study button and the list of its words.
 *
 * @param onBack called when the back button is pressed.
 * @param onStudy called with (deck, chapter, part) ids to start a session; [Routes.NONE] stands for "all".
 */
@Composable
fun ChapterScreen(onBack: () -> Unit, onStudy: (Long, Long, Long) -> Unit) {
    val vm = appViewModel { app, handle -> ChapterViewModel(app.library, app.settings, handle["chapterId"]!!) }
    val chapter by vm.chapter.collectAsState()
    val deck by vm.deck.collectAsState()
    val summary by vm.summary.collectAsState()
    val cards by vm.cards.collectAsState()
    val prefs by vm.prefs.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    var resetCard by remember { mutableStateOf<CardEntity?>(null) }
    val totals = summary?.toTotals()
    DetailScaffold(chapter?.title.orEmpty(), "Reset chapter progress", onBack, onReset = { confirmReset = true }) {
        item { ChapterHeader(chapter, deck, prefs, totals, vm) { deck?.let { onStudy(it.id, vm.chapterId, Routes.NONE) } } }
        item { ListHeading("Words") }
        items(cards, key = { it.id }) { card ->
            CardRow(card, prefs.direction, System.currentTimeMillis(), prefs.sessionsToComplete) { resetCard = card }
            HorizontalDivider()
        }
    }
    ResetDialogs(confirmReset, { confirmReset = false }, resetCard, { resetCard = null }, vm)
}

/**
 * The top of the chapter screen: titles, progress, study options and the study button.
 *
 * @param chapter the chapter; null while loading.
 * @param deck the book it belongs to; null while loading.
 * @param prefs the current settings.
 * @param totals the chapter's progress; null while loading.
 * @param vm the chapter view model.
 * @param onStudy called when the study button is pressed.
 */
@Composable
private fun ChapterHeader(
    chapter: ChapterEntity?,
    deck: DeckEntity?,
    prefs: Prefs,
    totals: ProgressTotals?,
    vm: ChapterViewModel,
    onStudy: () -> Unit,
) {
    DetailHeader(
        chapter?.nativeTitle,
        chapter?.title,
        totals,
        options = { StudyOptions(deck, prefs, vm::setDirection, vm::toggleMode) },
        studyLabel = studyButtonLabel("chapter", totals?.due ?: 0),
        studyEnabled = deck != null,
        onStudy = onStudy,
    )
}

/**
 * The confirmation dialogs for resetting the chapter or one word.
 *
 * @param chapterPending whether the chapter reset dialog is open.
 * @param onChapterDismiss called when the chapter dialog closes.
 * @param card the word whose reset is pending, or null.
 * @param onCardDismiss called when the word dialog closes.
 * @param vm performs the confirmed reset.
 */
@Composable
private fun ResetDialogs(
    chapterPending: Boolean,
    onChapterDismiss: () -> Unit,
    card: CardEntity?,
    onCardDismiss: () -> Unit,
    vm: ChapterViewModel,
) {
    if (chapterPending) {
        ResetDialog("Reset this chapter?", "Study progress of all words in this chapter is cleared.", vm::resetChapter, onChapterDismiss)
    }
    card?.let {
        ResetDialog("Reset “${it.frontText}”?", "This word becomes new again.", { vm.resetCard(it.id) }, onCardDismiss)
    }
}

/**
 * One word in the list, with its status and a button to reset it.
 *
 * @param card the word.
 * @param direction which side is shown as the headline.
 * @param now the current time in epoch milliseconds.
 * @param needed the number of sessions that completes a word.
 * @param onReset called when the reset button is pressed.
 */
@Composable
private fun CardRow(card: CardEntity, direction: Direction, now: Long, needed: Int, onReset: () -> Unit) {
    val status = cardStatus(card, now, needed)
    ListItem(
        headlineContent = { Text(card.prompt(direction)) },
        supportingContent = { Text(card.answer(direction)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (status.due) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hasProgress(card)) IconButton(onReset) { Icon(Icons.Default.Refresh, "Reset word") }
            }
        },
    )
}
