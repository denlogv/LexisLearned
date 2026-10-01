package dev.denlogv.lexislearned.epub

/**
 * A section of the book before its text is cut out.
 *
 * @property title the section's title.
 * @property part the title of the part it belongs to, or null if it stands alone.
 * @property pos where the section starts, or null if the table of contents entry points nowhere.
 * @property boilerplate true if the section lies in Project Gutenberg licence text.
 */
internal class SectionDraft(val title: String, val part: String?, val pos: SpinePos?, val boilerplate: Boolean)

/**
 * The leaf entries below a table-of-contents entry, each as a section draft.
 *
 * @param layout where entries start in the text.
 * @param entry the entry to descend from; a leaf yields one draft.
 * @param prefix titles of intermediate levels to put in front of a leaf's title, or null.
 * @param part the part title to assign to every leaf, or null.
 * @return the drafts in table-of-contents order.
 */
internal fun draftLeaves(layout: TocLayout, entry: TocEntry, prefix: String?, part: String?): List<SectionDraft> {
    if (entry.children.isEmpty()) {
        val pos = layout.position(entry)
        val title = listOfNotNull(prefix, entry.title).joinToString(TITLE_SEPARATOR)
        return listOf(SectionDraft(title, part, pos, pos != null && layout.isBoilerplate(pos)))
    }
    val childPrefix = listOfNotNull(prefix, entry.title.takeIf { part != null }).joinToString(TITLE_SEPARATOR).ifEmpty { null }
    return entry.children.flatMap { draftLeaves(layout, it, childPrefix, part) }
}

/** Separator between the levels of a title such as "Part · Chapter". */
private const val TITLE_SEPARATOR = " · "
