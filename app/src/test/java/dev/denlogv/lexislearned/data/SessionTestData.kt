package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.PlannerSnapshot
import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode
import kotlin.random.Random

/** A scope: one chapter of deck 1. */
val chapterScope = SessionScope(1, chapterId = 5)

/** Settings of a session. */
val sessionSettings = SessionSettings(Direction.REVERSE, setOf(StudyMode.SELECT, StudyMode.TYPE), 2, 4, 3, false)

/** Counters of a session half way. */
val sessionProgress = SessionProgress(stepsDone = 6, correct = 4, wrong = 2, completedWords = 1, totalWords = 3)

/**
 * The plan of a session over the given cards, after a few answers.
 *
 * @param ids the cards.
 * @return the snapshot.
 */
fun plannerSnapshot(ids: List<Long> = listOf(10L, 11L, 12L)): PlannerSnapshot {
    val planner = SessionPlanner(ids, sessionSettings.modes, sessionSettings.rounds, Random(4), sessionSettings.pairSize)
    repeat(3) {
        val step = planner.next()!!
        planner.answered(step, step.cardIds.associateWith { false })
    }
    planner.next()
    return planner.snapshot()
}

/**
 * A saved session.
 *
 * @param scope what it covers.
 * @param savedAt when it was saved.
 * @param planner its plan.
 * @return the session.
 */
fun savedSession(scope: SessionScope = chapterScope, savedAt: Long = 1_000, planner: PlannerSnapshot = plannerSnapshot()) =
    SavedSession(scope, savedAt, sessionSettings, sessionProgress, planner)
