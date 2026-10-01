package dev.denlogv.lexislearned.epub

/**
 * One section of a book (usually a chapter), possibly inside a part.
 *
 * @property index 1-based position of the section in the book.
 * @property title the section's title.
 * @property text the section's plain text.
 * @property part the title of the part the section belongs to, or null if it stands alone.
 * @property skipReason why the section should not be selected by default (front or back matter, boilerplate, very short),
 *   or null if it looks like real content.
 */
data class EpubChapter(val index: Int, val title: String, val text: String, val part: String? = null, val skipReason: String? = null) {
    /** Number of words in [text]. Computed once because the review screen reads it on every redraw. */
    val words: Int = HtmlText.countWords(text)
}

/**
 * A book split into sections.
 *
 * @property title the book's title.
 * @property language the language code declared by the book, or empty if it declares none.
 * @property chapters all sections in reading order, including ones flagged to skip.
 */
data class EpubBook(val title: String, val language: String, val chapters: List<EpubChapter>) {
    /** Titles of the book's parts in order of appearance. */
    val parts: List<String> get() = chapters.mapNotNull { it.part }.distinct()

    /** Indexes of the sections that should be selected by default, that is those not flagged to skip. */
    val defaultSelection: Set<Int> get() = chapters.filter { it.skipReason == null }.map { it.index }.toSet()
}
