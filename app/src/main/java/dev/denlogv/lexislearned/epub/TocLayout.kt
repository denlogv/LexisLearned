package dev.denlogv.lexislearned.epub

import java.util.IdentityHashMap

/**
 * Where each table-of-contents entry starts in the book's text, and the text that belongs to it.
 *
 * A section's text runs from its entry's position to the next entry's position of any depth.
 *
 * @param roots the table of contents.
 * @param fileIndex the spine index of each file path.
 * @param spine the book's text.
 */
internal class TocLayout(roots: List<TocEntry>, private val fileIndex: Map<String, Int>, private val spine: SpineText) {
    private val positions = IdentityHashMap<TocEntry, SpinePos?>()
    private val marks: List<SpinePos> = roots.flatMap { allPositions(it) }.sorted()

    /**
     * Where an entry starts.
     *
     * @param entry the entry.
     * @return its position, or the position of its first positioned child if it has no target of its own, or null if neither
     *   exists.
     */
    fun position(entry: TocEntry): SpinePos? {
        if (positions.containsKey(entry)) return positions[entry]
        val file = entry.file?.let { fileIndex[it] }
        val pos = if (file == null) entry.children.firstNotNullOfOrNull { position(it) } else spine.locate(file, entry.fragment)
        positions[entry] = pos
        return pos
    }

    /**
     * The position of the next entry after a given one.
     *
     * @param pos a position.
     * @return the earliest entry position strictly after [pos], or null if it is the last.
     */
    fun nextMark(pos: SpinePos): SpinePos? = marks.firstOrNull { it > pos }

    /**
     * The text of the section starting at a position.
     *
     * @param pos the section's start.
     * @return the text up to the next table-of-contents entry.
     */
    fun textAt(pos: SpinePos): String = spine.text(pos, nextMark(pos))

    /**
     * The text between two positions.
     *
     * @param from the start.
     * @param to the end (exclusive), or null for the end of the book.
     * @return the plain text.
     */
    fun textBetween(from: SpinePos, to: SpinePos?): String = spine.text(from, to)

    /**
     * The length of the section starting at a position.
     *
     * @param pos the section's start.
     * @return its number of words.
     */
    fun wordsAt(pos: SpinePos): Int = HtmlText.countWords(textAt(pos))

    /**
     * Whether a position lies in a licence boilerplate file.
     *
     * @param pos the position.
     * @return true for Project Gutenberg boilerplate.
     */
    fun isBoilerplate(pos: SpinePos): Boolean = spine.isBoilerplate(pos.file)

    /**
     * The positions of an entry and all entries below it.
     *
     * @param entry the entry.
     * @return their positions, unsorted; entries without a position are left out.
     */
    private fun allPositions(entry: TocEntry): List<SpinePos> = listOfNotNull(position(entry)) + entry.children.flatMap { allPositions(it) }
}
