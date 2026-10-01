package dev.denlogv.lexislearned.epub

import dev.denlogv.lexislearned.format.FormatException
import java.io.InputStream

/**
 * Splits an EPUB into sections following its table of contents (nested or flat), cutting files at the table's anchors
 * and grouping chapters into parts where the book has them.
 *
 * The work is shared between small collaborators: [EpubArchive] (files), [PackageReader] (metadata and reading order),
 * [TocLoader] (table of contents), [TocLayout] (where entries start) and the section drafters.
 */
object EpubReader {
    private const val UNTITLED = "Untitled"

    /**
     * Reads a book.
     *
     * @param input the EPUB file contents; closed when done.
     * @return the book with its sections. Front and back matter is included but flagged with a skip reason.
     * @throws FormatException if the file is not a readable EPUB, is DRM protected, or has no chapters.
     */
    fun read(input: InputStream): EpubBook {
        val archive = EpubArchive.read(input)
        val pkg = PackageReader.read(archive)
        val html = pkg.spine.map { archive.text(it).orEmpty() }
        val roots = TocLoader.load(archive, pkg, html)
        val layout = TocLayout(roots, pkg.spine.withIndex().associate { (i, path) -> path to i }, SpineText(html))
        val chapters = toChapters(draftSections(roots, layout), layout)
        if (chapters.isEmpty()) throw FormatException("No readable chapters found in this EPUB")
        return EpubBook(pkg.title.ifBlank { UNTITLED }, pkg.language, chapters)
    }

    /**
     * Drafts the sections with the strategy that fits the table of contents.
     *
     * @param roots the table of contents.
     * @param layout where entries start in the text.
     * @return the section drafts in reading order.
     */
    private fun draftSections(roots: List<TocEntry>, layout: TocLayout): List<SectionDraft> =
        if (roots.any { it.children.isNotEmpty() }) NestedSectionDrafter(layout).draft(roots) else FlatSectionDrafter(layout).draft(roots)

    /**
     * Cuts out each section's text and decides whether it should be skipped by default.
     *
     * @param drafts the section drafts; ones without a position are dropped.
     * @param layout where entries start in the text.
     * @return the book's sections, numbered from 1.
     */
    private fun toChapters(drafts: List<SectionDraft>, layout: TocLayout): List<EpubChapter> =
        drafts.filter { it.pos != null }.mapIndexed { i, draft ->
            val text = layout.textAt(draft.pos!!)
            EpubChapter(
                index = i + 1,
                title = HtmlText.squashSpaces(draft.title),
                text = text,
                part = draft.part?.let(HtmlText::squashSpaces),
                skipReason = EpubStructure.skipReason(draft.title, HtmlText.countWords(text), draft.boilerplate),
            )
        }
}
