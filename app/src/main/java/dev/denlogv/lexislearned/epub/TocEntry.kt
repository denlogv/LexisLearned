package dev.denlogv.lexislearned.epub

/**
 * One entry of a book's table of contents.
 *
 * @property title the entry's label.
 * @property file the archive path of the target document, or null for a group label without a target.
 * @property fragment the anchor inside the target document, or null for the start of the file.
 * @property children the nested entries; empty for a leaf.
 */
internal class TocEntry(val title: String, val file: String?, val fragment: String?, val children: List<TocEntry> = emptyList())
