package dev.denlogv.lexislearned.data

/** SQL pieces shared by several DAO queries (annotation values must be compile-time constants). */
internal object SqlFragments {
    /** Sets a card's study progress back to the state of a never-studied card. */
    const val RESET_CARDS = "UPDATE cards SET reps = 0, ease = 2.5, intervalDays = 0, dueAt = NULL, lapses = 0, sessionsDone = 0"

    /** Summary of a deck (alias `d`). */
    const val DECK_SUMMARY = """SELECT d.id, d.title, d.nativeTitle,
        (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id) AS total,
        (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.sessionsDone >= :needed) AS completed,
        (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.sessionsDone > 0 AND c.sessionsDone < :needed) AS inProgress,
        (SELECT COALESCE(SUM(MIN(c.sessionsDone, :needed)), 0) FROM cards c WHERE c.deckId = d.id) AS units,
        :needed AS needed,
        (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.sessionsDone < :needed
            AND c.dueAt IS NOT NULL AND c.dueAt <= :now) AS due
        FROM decks d"""

    /** Summary of a chapter (alias `h`). */
    const val CHAPTER_SUMMARY = """SELECT h.id, h.partId, h.title, h.nativeTitle,
        (SELECT COUNT(*) FROM cards c WHERE c.chapterId = h.id) AS total,
        (SELECT COUNT(*) FROM cards c WHERE c.chapterId = h.id AND c.sessionsDone >= :needed) AS completed,
        (SELECT COUNT(*) FROM cards c WHERE c.chapterId = h.id AND c.sessionsDone > 0 AND c.sessionsDone < :needed) AS inProgress,
        (SELECT COALESCE(SUM(MIN(c.sessionsDone, :needed)), 0) FROM cards c WHERE c.chapterId = h.id) AS units,
        :needed AS needed,
        (SELECT COUNT(*) FROM cards c WHERE c.chapterId = h.id AND c.sessionsDone < :needed
            AND c.dueAt IS NOT NULL AND c.dueAt <= :now) AS due
        FROM chapters h"""
}
