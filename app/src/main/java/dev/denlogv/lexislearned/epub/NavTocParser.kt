package dev.denlogv.lexislearned.epub

/**
 * Reads the table of contents of an EPUB 3 book (the navigation document), following the nesting of its list items.
 *
 * @param block the HTML of the table-of-contents list.
 * @param dir the folder of the navigation document, used to resolve the links it contains.
 */
internal class NavTocParser private constructor(private val block: String, private val dir: String) {
    /**
     * A list item that has been opened but not yet closed.
     *
     * @property title the label, empty until the first link or span inside the item is seen.
     * @property href the link target, if the label was a link.
     * @property children the entries of nested list items closed so far.
     */
    private class OpenItem(var title: String = "", var href: String? = null, val children: MutableList<TocEntry> = ArrayList())

    private val roots = ArrayList<TocEntry>()
    private val stack = ArrayList<OpenItem>()
    private val tokens = tagPattern.findAll(block).toList()

    /**
     * Walks the tags and builds the entries.
     *
     * @return the top-level entries.
     */
    private fun run(): List<TocEntry> {
        for ((i, token) in tokens.withIndex()) {
            val closing = token.groupValues[1] == "/"
            when (token.groupValues[2].lowercase()) {
                "li" -> if (closing) closeItem() else stack += OpenItem()
                "a", "span" -> if (!closing) readLabel(i, token)
            }
        }
        return roots
    }

    /** Finishes the innermost open list item and attaches it to its parent (or the top level). */
    private fun closeItem() {
        val item = stack.removeLastOrNull() ?: return
        if (item.title.isBlank() && item.children.isEmpty()) return
        val entry = TocEntry(
            title = item.title.trim(),
            file = item.href?.let { EpubPaths.resolve(dir, it) },
            fragment = item.href?.let(EpubPaths::fragmentOf),
            children = item.children.toList(),
        )
        (stack.lastOrNull()?.children ?: roots) += entry
    }

    /**
     * Takes the label (and link target) of the innermost open item from an `a` or `span` tag, unless it already has one.
     *
     * @param index the position of the opening tag in [tokens].
     * @param tag the opening tag.
     */
    private fun readLabel(index: Int, tag: MatchResult) {
        val item = stack.lastOrNull() ?: return
        if (item.title.isNotEmpty()) return
        val name = tag.groupValues[2]
        val close = tokens.drop(index + 1).firstOrNull { it.groupValues[1] == "/" && it.groupValues[2].equals(name, ignoreCase = true) }
        val inner = block.substring(tag.range.last + 1, close?.range?.first ?: (tag.range.last + 1))
        item.title = HtmlText.toPlain(inner).replace('\n', ' ')
        if (name.equals("a", ignoreCase = true)) item.href = hrefPattern.find(tag.groupValues[3])?.groupValues?.get(1)
    }

    companion object {
        private val tagPattern = Regex("(?is)<(/?)(li|a|span)\\b([^>]*)>")
        private val hrefPattern = Regex("href\\s*=\\s*\"([^\"]*)\"")
        private val tocNav = Regex("(?is)<nav\\b[^>]*epub:type=\"toc\"[^>]*>(.*?)</nav>")
        private val anyNav = Regex("(?is)<nav\\b[^>]*>(.*?)</nav>")

        /**
         * Parses a navigation document.
         *
         * @param html the document; the `toc` navigation block is used if present, else the first `nav`, else everything.
         * @param dir the folder of the document, used to resolve links.
         * @return the top-level entries with their nested entries.
         */
        fun parse(html: String, dir: String): List<TocEntry> {
            val block = tocNav.find(html)?.groupValues?.get(1) ?: anyNav.find(html)?.groupValues?.get(1) ?: html
            return NavTocParser(block, dir).run()
        }
    }
}
