package dev.denlogv.lexislearned.ui.deck

import dev.denlogv.lexislearned.data.ChapterSummary
import dev.denlogv.lexislearned.data.PartEntity

/**
 * Progress numbers added up over a group of chapters (a part or a whole book).
 *
 * @property total all words.
 * @property completed words that have reached the required number of sessions.
 * @property inProgress words with some but not all required sessions.
 * @property units the sum over all words of min(sessions, needed).
 * @property needed the number of sessions that completes a word.
 * @property due words due for review.
 */
data class ProgressTotals(val total: Int, val completed: Int, val inProgress: Int, val units: Int, val needed: Int, val due: Int) {
    /** Progress from 0 to 1. */
    val progress: Float get() = if (total == 0 || needed == 0) 0f else units.toFloat() / (total * needed)
}

/**
 * The progress of a single chapter.
 *
 * @receiver the chapter.
 * @return its totals.
 */
fun ChapterSummary.toTotals(): ProgressTotals = ProgressTotals(total, completed, inProgress, units, needed, due)

/**
 * Adds up the progress of a list of chapters.
 *
 * @receiver the chapters.
 * @return their combined totals; `needed` is taken from the first chapter and is 0 for an empty list.
 */
fun List<ChapterSummary>.totals(): ProgressTotals = ProgressTotals(
    total = sumOf { it.total },
    completed = sumOf { it.completed },
    inProgress = sumOf { it.inProgress },
    units = sumOf { it.units },
    needed = firstOrNull()?.needed ?: 0,
    due = sumOf { it.due },
)

/** A row on the book screen: a part with progress rolled up from its chapters, or a chapter outside any part. */
sealed interface DeckEntry {
    /** A stable key for the list. */
    val key: String

    /**
     * A part.
     *
     * @property part the part.
     * @property chapters how many chapters it has.
     * @property totals the progress of its chapters added up.
     */
    data class PartRow(val part: PartEntity, val chapters: Int, val totals: ProgressTotals) : DeckEntry {
        override val key: String get() = "p${part.id}"
    }

    /**
     * A chapter that belongs to no part.
     *
     * @property chapter the chapter.
     */
    data class ChapterRow(val chapter: ChapterSummary) : DeckEntry {
        override val key: String get() = "c${chapter.id}"
    }
}

/**
 * Lays out the book screen: chapters in reading order, with the chapters of each part replaced by one row for the part at
 * the place of the part's first chapter.
 *
 * @param parts the book's parts.
 * @param chapters the book's chapters in reading order.
 * @return the rows to show.
 */
fun buildEntries(parts: List<PartEntity>, chapters: List<ChapterSummary>): List<DeckEntry> {
    val partsById = parts.associateBy { it.id }
    val shown = HashSet<Long>()
    return chapters.mapNotNull { chapter ->
        val part = chapter.partId?.let { partsById[it] } ?: return@mapNotNull DeckEntry.ChapterRow(chapter)
        if (!shown.add(part.id)) return@mapNotNull null
        val inPart = chapters.filter { it.partId == part.id }
        DeckEntry.PartRow(part, inPart.size, inPart.totals())
    }
}
