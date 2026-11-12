package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.SessionProgress
import dev.denlogv.lexislearned.data.SessionScope
import dev.denlogv.lexislearned.data.SessionSettings
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.domain.SessionPlanner
import kotlin.random.Random

/**
 * A session that is ready to run.
 *
 * @property settings the settings the session runs with.
 * @property progress the counters to show; all zero for a new session.
 * @property planner the plan of the session.
 * @property cards the words with steps left.
 */
class SessionStart(val settings: SessionSettings, val progress: SessionProgress, val planner: SessionPlanner, val cards: List<CardEntity>)

/**
 * Prepares the session of a scope: chooses its words and plans their steps.
 *
 * @param study where the words are loaded from.
 * @param random source of randomness for the order of the steps.
 */
class SessionStarter(private val study: StudyRepository, private val random: Random) {
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
}
