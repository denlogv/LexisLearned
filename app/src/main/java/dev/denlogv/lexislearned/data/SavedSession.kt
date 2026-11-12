package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.PlannerSnapshot
import dev.denlogv.lexislearned.domain.StudyMode
import kotlinx.serialization.Serializable

/**
 * What a study session covers: a whole book, one part of it or one chapter. A session left half way is found again by its scope,
 * so the session of one chapter is never offered for another.
 *
 * @property deckId the book.
 * @property chapterId the chapter, or null if the session is not limited to one.
 * @property partId the part, or null if the session is not limited to one.
 */
@Serializable
data class SessionScope(val deckId: Long, val chapterId: Long? = null, val partId: Long? = null) {
    /** A name for the scope that is safe in a file name and different for every scope. */
    val key: String get() = "$deckId-${chapterId ?: ALL}-${partId ?: ALL}"

    private companion object {
        const val ALL = "all"
    }
}

/**
 * The settings a session was started with. They are kept with the session so that it continues the way it began, even if the
 * learner changed the study options in between.
 *
 * @property direction which side of a card is the question.
 * @property modes the selected study modes.
 * @property rounds how many times each word goes through all modes.
 * @property pairSize the most word pairs on a Pair board.
 * @property sessionsToComplete how many successful sessions make a word completed.
 * @property spaceSessions whether a word's sessions are spaced over days.
 */
@Serializable
data class SessionSettings(
    val direction: Direction,
    val modes: Set<StudyMode>,
    val rounds: Int,
    val pairSize: Int,
    val sessionsToComplete: Int,
    val spaceSessions: Boolean,
) {
    /** Reads the session part of the user's settings. */
    companion object {
        /**
         * Takes over what a session depends on.
         *
         * @param prefs the user's settings.
         * @return the settings of a session started now.
         */
        fun of(prefs: Prefs): SessionSettings = SessionSettings(
            direction = prefs.direction,
            modes = prefs.modes,
            rounds = prefs.rounds,
            pairSize = prefs.pairSize,
            sessionsToComplete = prefs.sessionsToComplete,
            spaceSessions = prefs.spaceSessions,
        )
    }
}

/**
 * The counters of a session, as the study screen shows them.
 *
 * @property stepsDone steps finished so far.
 * @property correct answers that were right.
 * @property wrong answers that were wrong.
 * @property completedWords words that finished all their steps.
 * @property totalWords how many words are in the session.
 */
@Serializable
data class SessionProgress(val stepsDone: Int, val correct: Int, val wrong: Int, val completedWords: Int, val totalWords: Int)

/**
 * A session that was left before it was finished.
 *
 * @property scope what the session covers.
 * @property savedAt when it was saved, in epoch milliseconds.
 * @property settings the settings it was started with.
 * @property progress the counters shown on screen.
 * @property planner the plan: the steps every word still has.
 */
@Serializable
data class SavedSession(
    val scope: SessionScope,
    val savedAt: Long,
    val settings: SessionSettings,
    val progress: SessionProgress,
    val planner: PlannerSnapshot,
)
