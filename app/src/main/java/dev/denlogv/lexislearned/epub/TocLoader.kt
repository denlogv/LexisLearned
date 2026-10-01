package dev.denlogv.lexislearned.epub

/** Turns a parser and the bytes of a table-of-contents file into entries. The second argument is the file's folder. */
private typealias TocParse = (ByteArray, String) -> List<TocEntry>

/** Finds the best table of contents of a book. */
internal object TocLoader {
    private const val USABLE_SIZE = 2

    /**
     * Loads the table of contents.
     *
     * Both the EPUB 2 (NCX) and EPUB 3 (navigation document) tables are tried and the larger one is used. Without a usable
     * table every file becomes its own entry. A single entry wrapping the whole book is unwrapped.
     *
     * @param archive the opened EPUB.
     * @param pkg the package document.
     * @param html the HTML of the spine files, in spine order, used to name entries when there is no table.
     * @return the top-level entries.
     */
    fun load(archive: EpubArchive, pkg: PackageDocument, html: List<String>): List<TocEntry> {
        val parsed = bestParsed(archive, pkg).ifEmpty { perFileEntries(pkg.spine, html) }
        return unwrapSingleRoot(parsed)
    }

    /**
     * Parses the available tables of contents and keeps the largest.
     *
     * @param archive the opened EPUB.
     * @param pkg the package document naming the tables.
     * @return the entries of the largest table, or empty if none could be read.
     */
    private fun bestParsed(archive: EpubArchive, pkg: PackageDocument): List<TocEntry> {
        var best = emptyList<TocEntry>()
        for ((path, parse) in sources(pkg)) {
            val parsed = parseOrEmpty(archive, path, parse)
            if (count(parsed) > count(best)) best = parsed
            if (count(best) >= USABLE_SIZE) break
        }
        return best
    }

    /**
     * Parses one table-of-contents file; a missing or unreadable file counts as an empty table.
     *
     * @param archive the opened EPUB.
     * @param path the file's path in the archive.
     * @param parse the parser for this kind of table.
     * @return the entries, or empty.
     */
    private fun parseOrEmpty(archive: EpubArchive, path: String, parse: TocParse): List<TocEntry> {
        val bytes = archive.bytes(path) ?: return emptyList()
        return runCatching { parse(bytes, EpubPaths.directoryOf(path)) }.getOrDefault(emptyList())
    }

    /**
     * The table-of-contents files of a book with the parser for each, most structured first.
     *
     * @param pkg the package document.
     * @return pairs of archive path and parser.
     */
    private fun sources(pkg: PackageDocument): List<Pair<String, TocParse>> = listOfNotNull(
        pkg.ncxPath?.let { it to { bytes: ByteArray, dir: String -> NcxTocParser.parse(parseXml(bytes), dir) } },
        pkg.navPath?.let { it to { bytes: ByteArray, dir: String -> NavTocParser.parse(String(bytes), dir) } },
    )

    /**
     * Fallback table of contents: one entry per spine file, named after its first heading or file name.
     *
     * @param spine the spine file paths.
     * @param html the HTML of those files, in the same order.
     * @return one entry per file.
     */
    private fun perFileEntries(spine: List<String>, html: List<String>): List<TocEntry> = spine.mapIndexed { i, path ->
        TocEntry(HtmlText.firstHeading(html[i]) ?: path.substringAfterLast('/'), path, null)
    }

    /**
     * Removes single-entry wrappers: a lone top-level entry with children is the book itself, not a part.
     *
     * @param roots the top-level entries.
     * @return the entries with wrappers removed.
     */
    private fun unwrapSingleRoot(roots: List<TocEntry>): List<TocEntry> {
        var current = roots
        while (current.size == 1 && current[0].children.isNotEmpty()) current = current[0].children
        return current
    }

    /**
     * Counts entries at all depths.
     *
     * @param entries the entries to count.
     * @return the total number of entries including nested ones.
     */
    private fun count(entries: List<TocEntry>): Int = entries.sumOf { 1 + count(it.children) }
}
