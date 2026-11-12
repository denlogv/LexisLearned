package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.Scheduler
import dev.denlogv.lexislearned.domain.SessionGrading
import dev.denlogv.lexislearned.domain.Sm2Scheduler

/**
 * What a study session needs from the database: choosing its words, finding wrong answers and filler words, and saving the
 * result of a finished session.
 *
 * @param db the database.
 * @param scheduler decides when a word is due after a session.
 * @param clock the current time in epoch milliseconds; tests replace it.
 */
class StudyRepository(
    db: AppDatabase,
    private val scheduler: Scheduler = Sm2Scheduler,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val cards = db.cardDao()

    /**
     * Chooses the words for a session: due words first, then new ones.
     *
     * @param deckId the deck.
     * @param chapterId restrict to this chapter, or null for all.
     * @param partId restrict to this part, or null for all.
     * @param needed the number of sessions that completes a word; completed words are not offered.
     * @param shape how many words to take: a fixed number from the whole scope, or everything of one chapter.
     * @return the session's words; empty when nothing in the scope can be studied.
     */
    suspend fun sessionCards(deckId: Long, chapterId: Long?, partId: Long?, needed: Int, shape: SessionShape): List<CardEntity> =
        when (shape) {
            is SessionShape.Capped -> cappedCards(deckId, chapterId ?: NO_FILTER, partId ?: NO_FILTER, needed, shape)
            SessionShape.WholeChapter -> chapterCards(deckId, chapterId, partId ?: NO_FILTER, needed)
        }

    /**
     * A fixed number of words from the whole scope.
     *
     * @param deckId the deck.
     * @param chapter the chapter, or [NO_FILTER].
     * @param part the part, or [NO_FILTER].
     * @param needed the number of sessions that completes a word.
     * @param limits the most words and the most new words.
     * @return the due words followed by the new ones.
     */
    private suspend fun cappedCards(deckId: Long, chapter: Long, part: Long, needed: Int, limits: SessionShape.Capped): List<CardEntity> {
        val due = cards.dueCards(deckId, chapter, part, needed, clock(), limits.size)
        val fresh = cards.newCards(deckId, chapter, part, minOf(limits.newLimit, limits.size - due.size).coerceAtLeast(0))
        return due + fresh
    }

    /**
     * How many more word sessions it takes to complete every word in a book, part or chapter. A new word needs all of the
     * sessions, a word in progress only those it lacks.
     *
     * @param deckId the deck.
     * @param chapterId restrict to this chapter, or null for all.
     * @param partId restrict to this part, or null for all.
     * @param needed the number of sessions that completes a word.
     * @return the sessions still to do, summed over the words; 0 when everything is completed.
     */
    suspend fun remainingSessions(deckId: Long, chapterId: Long?, partId: Long?, needed: Int): Int =
        cards.remainingSessions(deckId, chapterId ?: NO_FILTER, partId ?: NO_FILTER, needed)

    /**
     * Every studyable word of one chapter: the given one, or the first chapter of the scope that has any word to study.
     *
     * @param deckId the deck.
     * @param chapterId the chapter, or null to take the first one with words to study.
     * @param part the part, or [NO_FILTER].
     * @param needed the number of sessions that completes a word.
     * @return the due words followed by the new ones; empty if the chapter, or the whole scope, has none.
     */
    private suspend fun chapterCards(deckId: Long, chapterId: Long?, part: Long, needed: Int): List<CardEntity> {
        val now = clock()
        val chapter = chapterId ?: cards.firstStudyableChapter(deckId, part, needed, now) ?: return emptyList()
        return cards.dueCards(deckId, chapter, part, needed, now, NO_LIMIT) + cards.newCards(deckId, chapter, part, NO_LIMIT)
    }

    /**
     * Loads the words of a session that was left, as they are now. A word that was deleted or completed in the meantime (for
     * example by a session over a chapter of the same book) is not returned; one that was reset is, with its progress cleared.
     *
     * @param ids the words the session planned.
     * @param needed the number of sessions that completes a word.
     * @return the words that can still be studied, in reading order.
     */
    suspend fun resumableCards(ids: List<Long>, needed: Int): List<CardEntity> =
        ids.chunked(QUERY_CHUNK).flatMap { cards.openCards(it, needed) }.sortedBy { it.id }

    /**
     * Wrong answers for Select: other words from the same chapter first, the rest of the book only if needed.
     *
     * @param card the word being asked.
     * @param count how many wrong answers are wanted.
     * @return up to [count] other words.
     */
    suspend fun distractors(card: CardEntity, count: Int): List<CardEntity> = decoys(card, listOf(card.id), count)

    /**
     * Filler words for the Pair board: random words from the same chapter as [card], topped up from the rest of the book
     * only when the chapter has too few.
     *
     * @param card a word whose chapter to draw from.
     * @param excludeIds words that must not be returned.
     * @param count how many words are wanted.
     * @return up to [count] words.
     */
    suspend fun decoys(card: CardEntity, excludeIds: List<Long>, count: Int): List<CardEntity> {
        val exclude = excludeIds.ifEmpty { listOf(NO_CARD) }
        val same = cards.randomInChapter(card.chapterId, exclude, count)
        if (same.size >= count) return same
        return same + cards.randomCardsExcluding(card.deckId, exclude + same.map { it.id }, count - same.size)
    }

    /**
     * Filler words for the Pair board: only words that were studied before, so the board never shows a word the learner has
     * not met yet. They come from the same chapter as [card] first and from the rest of the book only when the chapter has too
     * few.
     *
     * @param card a word whose chapter to draw from.
     * @param excludeIds words that must not be returned.
     * @param count how many words are wanted.
     * @return up to [count] studied words; fewer when the book has too few of them.
     */
    suspend fun learnedFillers(card: CardEntity, excludeIds: List<Long>, count: Int): List<CardEntity> {
        val exclude = excludeIds.ifEmpty { listOf(NO_CARD) }
        val same = cards.randomLearnedInChapter(card.chapterId, exclude, count)
        if (same.size >= count) return same
        return same + cards.randomLearnedInDeck(card.deckId, exclude + same.map { it.id }, count - same.size)
    }

    /**
     * Saves the result of one finished session for a word. The session counts toward completion unless it went badly.
     *
     * @param card the word before the session.
     * @param grade the session's grade, from the first-try accuracy.
     * @param spaced if true spaced-repetition intervals apply, otherwise the word can be studied again straight away.
     * @return the word with its new state.
     */
    suspend fun finishSession(card: CardEntity, grade: Grade, spaced: Boolean): CardEntity {
        val now = clock()
        val next = scheduler.review(card.progress, grade, now)
        val counted = SessionGrading.counts(grade)
        val sessions = card.sessionsDone + if (counted) 1 else 0
        val dueAt = if (!spaced && counted) now else next.dueAt
        cards.updateProgress(card.id, next.reps, next.ease, next.intervalDays, dueAt, next.lapses, sessions)
        return card.copy(
            reps = next.reps,
            ease = next.ease,
            intervalDays = next.intervalDays,
            dueAt = dueAt,
            lapses = next.lapses,
            sessionsDone = sessions,
        )
    }

    private companion object {
        /** The most ids in one query; SQLite allows 999 values in older versions. */
        const val QUERY_CHUNK = 500

        /** Passed to queries to mean "do not filter". */
        const val NO_FILTER = -1L

        /** Passed as a limit to mean "all of them". */
        const val NO_LIMIT = Int.MAX_VALUE

        /** An id no card has, so an exclusion list is never empty. */
        const val NO_CARD = -1L
    }
}
