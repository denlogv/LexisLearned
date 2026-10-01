package dev.denlogv.lexislearned.epub

import org.w3c.dom.Element

/** Reads the table of contents of an EPUB 2 book (the NCX file). */
internal object NcxTocParser {
    /**
     * Parses the navigation map.
     *
     * @param root the root element of the NCX document.
     * @param dir the folder of the NCX file, used to resolve the links it contains.
     * @return the top-level entries, each with its nested entries.
     */
    fun parse(root: Element, dir: String): List<TocEntry> {
        val map = root.childElements("navMap").firstOrNull() ?: root
        return map.childElements("navPoint").map { entry(it, dir) }
    }

    /**
     * Converts one `navPoint` element and its nested ones.
     *
     * @param navPoint the element.
     * @param dir the folder used to resolve links.
     * @return the entry.
     */
    private fun entry(navPoint: Element, dir: String): TocEntry {
        val label = navPoint.childElements("navLabel").firstOrNull()?.descendants("text")?.firstOrNull()?.textContent.orEmpty()
        val src = navPoint.childElements("content").firstOrNull()?.getAttribute("src").orEmpty()
        return TocEntry(
            title = HtmlText.squashSpaces(label),
            file = src.takeIf { it.isNotEmpty() }?.let { EpubPaths.resolve(dir, it) },
            fragment = EpubPaths.fragmentOf(src),
            children = navPoint.childElements("navPoint").map { entry(it, dir) },
        )
    }
}
