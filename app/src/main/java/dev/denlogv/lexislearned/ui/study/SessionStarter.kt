package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.SavedSession
import dev.denlogv.lexislearned.data.SessionKeeper
import dev.denlogv.lexislearned.data.SessionProgress
import dev.denlogv.lexislearned.data.SessionScope
import dev.denlogv.lexislearned.data.SessionSettings
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.domain.SessionPlanner
import kotlin.random.Random

/**
 * A session that is ready to run, either planned from the database or restored from a saved one.
 *
 * @property settings the settings the session runs with.
 * @property progress the counters to show; all zero for a new session.
 * @property planner the plan of the session.
 * @property cards the words with steps left, as the database has them now.
 */
class SessionStart(val settings: SessionSettings, val progress: SessionProgress, val planner: SessionPlanner, val cards: List<CardEntity>)

/**
 * Prepares the session of a scope: plans a new one, or turns the one that was left unfinished into a session that can be
 * continued, checking it against the database as it is now.
 *
 * @param study where the words are loaded from.
 * @param keeper where unfinished sessions are kept.
 * @param random source of randomness for the order of the steps.
 */
class SessionStarter(private val study: StudyRepository, private val keeper: SessionKeeper, private val random: Random) {
    /**
     * Looks for a session of the scope that was left unfinished and can still be continued.
     *
     * @param scope what the session covers.
     * @return the session, or null if there is none, it is too old or none of its words can be studied any more.
     */
    suspend fun unfinished(scope: SessionScope): SessionStart? = keeper.find(scope)?.let { restore(it) }

    /**
     * Plans a new session: due words first, then new ones.
     *
     * @param scope what the session covers.
     * @param prefs the user's settings, which decide the settings and the size of the session.
     * @return the session, with no words if there is nothing to study.
     */
    suspend fun fresh(scope: SessionScope, prefs: Prefs): SessionStart {
        val settings = SessionSettings.of(prefs)
        val list = study.sessionCards(scope.deckId, scope.chapterId, scope.partId, settings.sessionsToComplete, prefs.sessionShape)
        val planner = SessionPlanner(list.map { it.id }, settings.modes, settings.rounds, random, settings.pairSize)
        return SessionStart(settings, SessionProgress(0, 0, 0, 0, list.size), planner, list)
    }

    /**
     * Restores a saved session. Words that were deleted or completed since are dropped from the plan and from the word count.
     *
     * @param saved the session as it was saved.
     * @return the session to continue, or null if none of its words can be studied any more.
     */
    private suspend fun restore(saved: SavedSession): SessionStart? {
        val planned = saved.planner.plans.map { it.id }
        val cards = study.resumableCards(planned, saved.settings.sessionsToComplete)
        val planner = SessionPlanner.restore(saved.planner, random, cards.map { it.id }.toSet())
        if (planner.activeCards == 0) return null
        val total = (saved.progress.totalWords - (planned.size - planner.activeCards)).coerceAtLeast(0)
        return SessionStart(saved.settings, saved.progress.copy(totalWords = total), planner, cards)
    }
}
