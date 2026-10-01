package dev.denlogv.lexislearned.epub

/**
 * Drafts sections for a book whose table of contents is flat, grouping chapters under part headings found by
 * [EpubStructure.detectHeaders].
 *
 * @param layout where entries start in the text.
 */
internal class FlatSectionDrafter(private val layout: TocLayout) {
    /**
     * Drafts all sections.
     *
     * @param roots the table-of-contents entries.
     * @return the section drafts in reading order.
     */
    fun draft(roots: List<TocEntry>): List<SectionDraft> {
        val flat = roots.flatMap { draftLeaves(layout, it, null, null) }
        val words = flat.map { draft -> draft.pos?.let(layout::wordsAt) ?: 0 }
        val leaves = flat.indices.map { EpubStructure.Leaf(flat[it].title, words[it]) }
        val headers = EpubStructure.detectHeaders(leaves)
        return flat.indices.mapNotNull { draftFor(flat, words, headers, it) }
    }

    /**
     * Drafts one entry according to the part heading it falls under.
     *
     * @param flat all entries.
     * @param words the length of each entry in words.
     * @param headers for each entry the index of its heading, or null when it sits before any group.
     * @param i the index of the entry to draft.
     * @return the entry under its part; a heading only if it holds more than a title page; null to drop a bare heading.
     */
    private fun draftFor(flat: List<SectionDraft>, words: List<Int>, headers: List<Int?>, i: Int): SectionDraft? {
        val entry = flat[i]
        return when (val header = headers[i]) {
            null -> entry
            i -> entry.takeIf { words[i] >= EpubStructure.INTRO_WORDS }?.let { SectionDraft(it.title, it.title, it.pos, it.boilerplate) }
            else -> SectionDraft(entry.title, flat[header].title, entry.pos, entry.boilerplate)
        }
    }
}
