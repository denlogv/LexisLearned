package dev.denlogv.lexislearned.epub

/** Book-structure heuristics, kept free of zip and XML so they can be tested on plain titles and word counts. */
object EpubStructure {
    /** Sections shorter than this many words are flagged as very short. */
    const val MIN_WORDS = 150

    /** A part heading is kept as a chapter of its own only if it has at least this many words. */
    const val INTRO_WORDS = 4 * MIN_WORDS

    private const val CHAPTER_WORDS = "chapter|chap\\.?|kapitel|chapitre|cap[ií]tulo|capitolo|глава"

    private val nonContent = Regex(
        "^\\s*(contents|table of contents|copyright|about (the|this)|acknowledg|also by|other books|praise|" +
            "reading group|q\\s*&\\s*a|title page|half.?title|cover|dedication|imprint|impressum|inhalt|sommaire|" +
            "table des mati|notes?$|endnotes|footnotes|bibliograph|references|further reading|index|glossary|" +
            "permissions|credits|colophon|a note on|list of (figures|illustrations|tables)|" +
            "the full project gutenberg)",
        RegexOption.IGNORE_CASE,
    )
    private val framing = Regex(
        "^\\s*(foreword|preface|introduction|prologue|epilogue|afterword|postscript|author'?s? note|note|" +
            "conclusions?|appendix|preambule|vorwort|nachwort|prefacio|prefazione)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val partWord = Regex("^\\s*(part|book|volume|act|section)\\b", RegexOption.IGNORE_CASE)
    private val numbered = Regex("^\\s*(?:(?:$CHAPTER_WORDS)\\s*)?0*(\\d{1,4})\\b", RegexOption.IGNORE_CASE)
    private val sortPrefix = Regex("^0*\\d{1,3}\\s+(?=(?:$CHAPTER_WORDS)\\s*\\d)", RegexOption.IGNORE_CASE)

    /**
     * A table-of-contents entry as seen by the grouping heuristic.
     *
     * @property title the entry's title.
     * @property words the length of the entry's section in words.
     */
    data class Leaf(val title: String, val words: Int)

    /**
     * Why a section should not be selected by default.
     *
     * @param title the section's title.
     * @param words the section's length in words.
     * @param boilerplate true if the section is Project Gutenberg licence text.
     * @return a short reason, or null if the section looks like real content.
     */
    fun skipReason(title: String, words: Int, boilerplate: Boolean): String? = when {
        boilerplate -> "Project Gutenberg boilerplate"
        nonContent.containsMatchIn(title) -> "front/back matter"
        words < MIN_WORDS -> "very short ($words words)"
        else -> null
    }

    /**
     * Whether a title is introductory or closing matter (foreword, prologue, appendix and the like), which stays a loose
     * chapter instead of becoming a part of its own.
     *
     * @param title the section's title.
     * @return true for framing matter.
     */
    fun isFraming(title: String): Boolean = framing.containsMatchIn(title)

    /**
     * The chapter number in a title such as "Chapter 12", "12: Anchors" or "01 Chapter 1".
     *
     * @param title the section's title.
     * @return the number, or null if the title is not numbered.
     */
    fun chapterNumber(title: String): Int? {
        val t = sortPrefix.replaceFirst(title.trim(), "")
        return numbered.find(t)?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * For a table of contents without nesting, finds the entries that head a group of chapters: those titled "Part 2",
     * "Book Three" and so on, and, when the book has several numbered runs, the entry right before a numbering restart
     * ("Chapter 1" after "Chapter 35").
     *
     * @param leaves the entries in reading order.
     * @return for each entry the index of its group's heading, or null when it sits before any group. A heading maps to
     *   itself.
     */
    fun detectHeaders(leaves: List<Leaf>): List<Int?> {
        val isHeader = BooleanArray(leaves.size)
        markPartTitles(leaves, isHeader)
        markNumberingRestarts(leaves, isHeader)
        var current: Int? = null
        return leaves.indices.map { i ->
            if (isHeader[i]) current = i
            current
        }
    }

    /**
     * Marks entries titled like a part ("Part 1", "Book II") that are short enough to be a title page.
     *
     * @param leaves the entries.
     * @param isHeader the marks; updated in place.
     */
    private fun markPartTitles(leaves: List<Leaf>, isHeader: BooleanArray) {
        for (i in leaves.indices) {
            if (partWord.containsMatchIn(leaves[i].title) && leaves[i].words < INTRO_WORDS) isHeader[i] = true
        }
    }

    /**
     * When chapter numbering starts over several times, marks the unnumbered entry before each restart as a heading.
     *
     * @param leaves the entries.
     * @param isHeader the marks; updated in place.
     */
    private fun markNumberingRestarts(leaves: List<Leaf>, isHeader: BooleanArray) {
        val runStarts = leaves.indices.filter { !isHeader[it] && chapterNumber(leaves[it].title) == 1 }
        if (runStarts.size < 2) return
        for (start in runStarts) {
            val before = start - 1
            if (before >= 0 && !isHeader[before] && chapterNumber(leaves[before].title) == null) isHeader[before] = true
        }
    }
}
