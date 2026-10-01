package dev.denlogv.lexislearned.ui.epub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.ai.Languages
import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.epub.EpubBook
import dev.denlogv.lexislearned.epub.EpubChapter
import dev.denlogv.lexislearned.ui.settings.LevelChips
import kotlin.math.roundToInt

/** Languages offered for the book. */
private val LANGUAGES = listOf("en", "de", "fr", "es", "it", "pt", "nl", "pl", "sv", "cs", "tr", "uk", "ru")

/** Indentation of sections that sit under a part. */
private val PART_INDENT = 28.dp

/** Indentation of sections that belong to no part. */
private val LOOSE_INDENT = 8.dp

/**
 * The review of the detected structure: language, level, density and the sections to turn into cards.
 *
 * @param book the book.
 * @param review the current choices.
 * @param level the user's level.
 * @param onChange called with a function that computes new choices from the current ones.
 * @param onLevel called with the level the user picks.
 * @param modifier layout modifier.
 */
@Composable
fun ReviewList(
    book: EpubBook,
    review: ReviewState,
    level: CefrLevel,
    onChange: ((ReviewState) -> ReviewState) -> Unit,
    onLevel: (CefrLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(book) { groupByPart(book.chapters) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            ReviewHeader(book, review, level, onChange, onLevel)
            HorizontalDivider()
        }
        groups.forEach { group -> groupItems(group, review, onChange) }
    }
}

/**
 * The top of the review: title, summary, language, level, density and quick selection buttons.
 *
 * @param book the book.
 * @param review the current choices.
 * @param level the user's level.
 * @param onChange called with a function that computes new choices from the current ones.
 * @param onLevel called with the level the user picks.
 */
@Composable
private fun ReviewHeader(
    book: EpubBook,
    review: ReviewState,
    level: CefrLevel,
    onChange: ((ReviewState) -> ReviewState) -> Unit,
    onLevel: (CefrLevel) -> Unit,
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(book.title, style = MaterialTheme.typography.titleLarge)
        Text(bookSummary(book))
        LanguageRow(review.lang) { lang -> onChange { it.copy(lang = lang) } }
        if (book.language.isBlank()) {
            Text(
                "The EPUB does not declare its language; please check it.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("Your level (cards cover words above it):", style = MaterialTheme.typography.labelLarge)
        LevelChips(level, onLevel)
        Text("Cards per 1,000 words: ${review.density}")
        Slider(review.density.toFloat(), { v -> onChange { it.copy(density = v.roundToInt()) } }, valueRange = 2f..20f, steps = 17)
        SelectionButtons(book, onChange)
    }
}

/**
 * The buttons that select all, none or the suggested sections.
 *
 * @param book the book.
 * @param onChange called with a function that computes new choices from the current ones.
 */
@Composable
private fun SelectionButtons(book: EpubBook, onChange: ((ReviewState) -> ReviewState) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton({ onChange { it.copy(selected = book.chapters.map { c -> c.index }.toSet()) } }) { Text("All") }
        TextButton({ onChange { it.copy(selected = emptySet()) } }) { Text("None") }
        TextButton({ onChange { it.copy(selected = book.defaultSelection) } }) { Text("Suggested") }
    }
}

/**
 * The book-language button with its menu.
 *
 * @param lang the selected language code.
 * @param onPick called with the language the user picks.
 */
@Composable
private fun LanguageRow(lang: String, onPick: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Book language:")
        Box {
            OutlinedButton({ menu = true }) { Text("${Languages.nameOf(lang)} ($lang)") }
            DropdownMenu(menu, { menu = false }) {
                LANGUAGES.forEach { l ->
                    DropdownMenuItem({ Text("${Languages.nameOf(l)} ($l)") }, {
                        onPick(l)
                        menu = false
                    })
                }
            }
        }
    }
}

/**
 * The list rows of one group: a part header, if the group is a part, followed by its sections.
 *
 * @param group the group.
 * @param review the current choices.
 * @param onChange called with a function that computes new choices from the current ones.
 */
private fun LazyListScope.groupItems(group: ChapterGroup, review: ReviewState, onChange: ((ReviewState) -> ReviewState) -> Unit) {
    val indent = if (group.part != null) PART_INDENT else LOOSE_INDENT
    if (group.part != null) {
        item(key = "part-${group.chapters.first().index}") { PartRow(group, review.selected) { onChange { it.togglePart(group) } } }
    }
    items(group.chapters, key = { it.index }) { c ->
        ChapterRow(c, c.index in review.selected, review.density, indent) { onChange { it.toggleChapter(c.index) } }
    }
}

/**
 * The header row of a part, with a checkbox for all its sections.
 *
 * @param group the part's sections.
 * @param selected indexes of the selected sections.
 * @param onToggle called when the row is tapped.
 */
@Composable
private fun PartRow(group: ChapterGroup, selected: Set<Int>, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(partToggleState(group, selected), null, Modifier.padding(8.dp))
        Column(Modifier.weight(1f)) {
            Text(group.part.orEmpty(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            val n = group.chapters.count { it.index in selected }
            Text("${group.chapters.size} chapter(s) · $n selected", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * The row of one section.
 *
 * @param chapter the section.
 * @param checked whether it is selected.
 * @param density cards per 1,000 words, for the card estimate.
 * @param indent the space to the left of the checkbox.
 * @param onToggle called when the row is tapped.
 */
@Composable
private fun ChapterRow(chapter: EpubChapter, checked: Boolean, density: Int, indent: Dp, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = indent, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, null, Modifier.padding(8.dp))
        Column(Modifier.weight(1f)) {
            Text(chapter.title, maxLines = 2)
            Text(
                chapterDetail(chapter, density),
                style = MaterialTheme.typography.labelSmall,
                color = if (chapter.skipReason != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
