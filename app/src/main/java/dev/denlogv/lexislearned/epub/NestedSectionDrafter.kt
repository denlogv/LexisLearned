package dev.denlogv.lexislearned.epub

/**
 * Drafts sections for a book whose table of contents is nested: top-level entries with children become parts.
 *
 * @param layout where entries start in the text.
 */
internal class NestedSectionDrafter(private val layout: TocLayout) {
    /**
     * Drafts all sections.
     *
     * @param roots the top-level entries.
     * @return the section drafts in reading order.
     */
    fun draft(roots: List<TocEntry>): List<SectionDraft> {
        val parted = roots.count { it.children.isNotEmpty() } >= 2
        return roots.flatMap { if (it.children.isEmpty()) draftLoose(it, parted) else draftPart(it) }
    }

    /**
     * Drafts an entry without children. Among parted siblings a real story without sections becomes a one-chapter part.
     *
     * @param entry the entry.
     * @param parted whether the book has parts (at least two top-level entries with children).
     * @return the entry's draft.
     */
    private fun draftLoose(entry: TocEntry, parted: Boolean): List<SectionDraft> {
        val pos = layout.position(entry)
        val standalone = parted && pos != null && isStory(entry.title, pos)
        return draftLeaves(layout, entry, null, entry.title.takeIf { standalone })
    }

    /**
     * Whether a childless entry is a real story rather than front or back matter.
     *
     * @param title the entry's title.
     * @param pos where it starts.
     * @return true if it is not introductory matter and has no reason to be skipped.
     */
    private fun isStory(title: String, pos: SpinePos): Boolean = !EpubStructure.isFraming(title) &&
        EpubStructure.skipReason(title, layout.wordsAt(pos), layout.isBoilerplate(pos)) == null

    /**
     * Drafts a part: an optional introduction chapter followed by its leaves.
     *
     * @param part the part's entry.
     * @return the drafts of the part's sections.
     */
    private fun draftPart(part: TocEntry): List<SectionDraft> =
        listOfNotNull(introduction(part)) + part.children.flatMap { draftLeaves(layout, it, null, part.title) }

    /**
     * The part's own text before its first child, if there is enough of it to be worth a chapter.
     *
     * @param part the part's entry.
     * @return a draft titled like the part, or null if there is little or no introductory text.
     */
    private fun introduction(part: TocEntry): SectionDraft? {
        val start = layout.position(part) ?: return null
        val firstChild = part.children.firstNotNullOfOrNull { layout.position(it) }
        val own = layout.textBetween(start, firstChild ?: layout.nextMark(start))
        if (HtmlText.countWords(own) < EpubStructure.MIN_WORDS) return null
        return SectionDraft(part.title, part.title, start, layout.isBoilerplate(start))
    }
}
