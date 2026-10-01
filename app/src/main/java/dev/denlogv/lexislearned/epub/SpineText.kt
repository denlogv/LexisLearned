package dev.denlogv.lexislearned.epub

/**
 * A position in the book's text: a spine file and an offset in its HTML.
 *
 * @property file index of the file in the spine.
 * @property offset character offset within the file's HTML.
 */
internal data class SpinePos(val file: Int, val offset: Int) : Comparable<SpinePos> {
    /**
     * Orders positions by reading order.
     *
     * @param other the position to compare with.
     * @return negative if this comes first, positive if it comes later, zero if equal.
     */
    override fun compareTo(other: SpinePos): Int = compareValuesBy(this, other, { it.file }, { it.offset })
}

/**
 * The HTML of a book's spine files, with cutting of text between two positions.
 *
 * @param html the HTML of each spine file, in reading order.
 */
internal class SpineText(private val html: List<String>) {
    private val boilerplate = html.map { BOILERPLATE_MARKER in it }
    private val cache = HashMap<Pair<SpinePos, SpinePos?>, String>()

    /**
     * Whether a file is Project Gutenberg licence boilerplate.
     *
     * @param file index of the file in the spine.
     * @return true if the file carries the boilerplate marker.
     */
    fun isBoilerplate(file: Int): Boolean = boilerplate[file]

    /**
     * Finds where a table-of-contents target starts.
     *
     * @param file index of the target file in the spine.
     * @param fragment the anchor inside the file, or null/empty for the start of the file.
     * @return the position of the anchor's tag, or the start of the file if the anchor is not found.
     */
    fun locate(file: Int, fragment: String?): SpinePos {
        val offset = fragment?.takeIf { it.isNotEmpty() }?.let { fragmentOffset(html[file], it) } ?: 0
        return SpinePos(file, offset)
    }

    /**
     * The plain text from one position up to another. Results are cached because sections are asked for repeatedly.
     *
     * @param from where to start.
     * @param to where to stop (exclusive), or null to read to the end of the book.
     * @return the text of everything in between, skipping boilerplate files other than the first.
     */
    fun text(from: SpinePos, to: SpinePos?): String = cache.getOrPut(from to to) { extract(from, to) }

    /**
     * Cuts the HTML between two positions and converts it to plain text.
     *
     * @param from where to start.
     * @param to where to stop (exclusive), or null for the end of the book.
     * @return the plain text.
     */
    private fun extract(from: SpinePos, to: SpinePos?): String {
        val sb = StringBuilder()
        val lastFile = to?.file ?: (html.size - 1)
        for (file in from.file..lastFile) {
            if (boilerplate[file] && file != from.file) continue
            val start = if (file == from.file) from.offset else 0
            val end = if (to != null && file == to.file) to.offset else html[file].length
            if (start < end) sb.append(html[file], start, end).append('\n')
        }
        return HtmlText.toPlain(sb.toString())
    }

    /**
     * Offset of the tag carrying a given id (or name) so that a section can start exactly there.
     *
     * @param page the file's HTML.
     * @param fragment the id or name to look for.
     * @return the offset of the opening `<` of that tag, or null if the anchor does not exist.
     */
    private fun fragmentOffset(page: String, fragment: String): Int? {
        val match = Regex("(?:id|name)\\s*=\\s*[\"']${Regex.escape(fragment)}[\"']").find(page) ?: return null
        val tagStart = page.lastIndexOf('<', match.range.first)
        return if (tagStart >= 0) tagStart else match.range.first
    }

    private companion object {
        /** Class name Project Gutenberg puts on its licence sections. */
        const val BOILERPLATE_MARKER = "pg-boilerplate"
    }
}
