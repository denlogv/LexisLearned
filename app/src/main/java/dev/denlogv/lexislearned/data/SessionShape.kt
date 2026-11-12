package dev.denlogv.lexislearned.data

/** How many words one study session takes from the book, part or chapter it was started from. */
sealed interface SessionShape {
    /**
     * Every studyable word of one chapter: the due words first, then the new ones, without a limit on either. When the
     * session starts from a part or a book it is the first chapter, in reading order, that has any word to study.
     */
    data object WholeChapter : SessionShape

    /**
     * A fixed number of words, taken from the whole scope.
     *
     * @property size the most words in the session.
     * @property newLimit the most never-studied words among them.
     */
    data class Capped(val size: Int, val newLimit: Int) : SessionShape
}
