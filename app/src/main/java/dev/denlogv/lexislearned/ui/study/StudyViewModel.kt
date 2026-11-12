package dev.denlogv.lexislearned.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.SessionKeeper
import dev.denlogv.lexislearned.data.SessionProgress
import dev.denlogv.lexislearned.data.SessionScope
import dev.denlogv.lexislearned.data.SessionSettings
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.SessionGrading
import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode
import dev.denlogv.lexislearned.ui.Routes
import kotlin.random.Random
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A study session: every word runs through every selected mode (repeated for the configured number of rounds) before the
 * session is done. The order comes from [SessionPlanner]; this class shows the steps and saves each word's result as soon as
 * that word has finished its whole plan.
 *
 * The state of the session is kept by [SessionKeeper] after every step. When the learner studies the same scope again after
 * leaving a session unfinished, the screen first offers to [continueSession] or to [startNewSession]; the words that finished
 * before are already saved to the database, so a continued session never saves them again.
 *
 * The navigation arguments `deckId`, `chapterId` and `partId` come from [handle]; [Routes.NONE] stands for "all".
 *
 * @param study the words of a session and where its results are saved.
 * @param settings the user's settings; they are read once when the view model is created.
 * @param handle the saved state holding the navigation arguments.
 * @param keeper where the unfinished session is kept.
 * @param random source of randomness for the order of steps and answer choices.
 * @param clock a monotonic clock in milliseconds that times the learner's steps; tests replace it.
 */
class StudyViewModel(
    private val study: StudyRepository,
    settings: Settings,
    handle: SavedStateHandle,
    private val keeper: SessionKeeper,
    private val random: Random = Random.Default,
    private val clock: () -> Long = SessionTimer.Companion::monotonicMillis,
) : ViewModel() {
    private val deckId: Long = handle["deckId"]!!
    private val chapterId: Long? = handle.get<Long>("chapterId")?.takeIf { it >= 0 }
    private val partId: Long? = handle.get<Long>("partId")?.takeIf { it >= 0 }
    private val target = SessionScope(deckId, chapterId, partId)
    private val prefs: Prefs = settings.prefs.value
    private val starter = SessionStarter(study, keeper, random)
    private lateinit var session: SessionSettings
    private lateinit var presenter: StepPresenter
    private val cards = HashMap<Long, CardEntity>()
    private var planner: SessionPlanner? = null
    private var step: SessionPlanner.Step? = null
    private var offered: SessionStart? = null
    private var timer: SessionTimer? = null
    private val scopeName = if (chapterId != null) {
        "chapter"
    } else if (partId != null) {
        "part"
    } else {
        "book"
    }
    private val _ui = MutableStateFlow(StudyUi())

    /** What the screen shows. */
    val ui: StateFlow<StudyUi> = _ui

    init {
        viewModelScope.launch { open() }
    }

    /**
     * Grades the current single-card step (Select, Check, Type, or Learn as a plain flashcard).
     *
     * @param grade AGAIN counts as a wrong answer; any other grade as a right one.
     */
    fun answer(grade: Grade) {
        val current = step ?: return
        val ok = grade != Grade.AGAIN
        _ui.update { if (ok) it.copy(correct = it.correct + 1) else it.copy(wrong = it.wrong + 1) }
        planner?.answered(current, mapOf(current.cardIds.first() to ok))
        proceed()
    }

    /**
     * Grades a Pair step.
     *
     * @param missed cards that had at least one wrong match; fillers are ignored.
     */
    fun answerPair(missed: Set<Long>) {
        val current = step ?: return
        val results = current.cardIds.associateWith { it !in missed }
        _ui.update { it.copy(correct = it.correct + results.count { r -> r.value }, wrong = it.wrong + results.count { r -> !r.value }) }
        planner?.answered(current, results)
        proceed()
    }

    /** Ends an introduction (Learn) step; it is not graded. */
    fun introduced() {
        val current = step ?: return
        planner?.introduced(current.cardIds.first())
        proceed()
    }

    /** "I know it": the word skips the rest of this session and counts as fully known. */
    fun known() {
        val current = step ?: return
        planner?.known(current.cardIds.first())
        proceed(timed = false)
    }

    /** Continues the unfinished session that is on offer, with the words, the plan, the counters and the settings it had. */
    fun continueSession() {
        val saved = offered ?: return
        offered = null
        _ui.update { it.copy(loading = true, offering = false) }
        viewModelScope.launch { run(saved) }
    }

    /** Throws the unfinished session that is on offer away and plans a new one from the database. */
    fun startNewSession() {
        if (offered == null) return
        offered = null
        _ui.update { StudyUi() }
        viewModelScope.launch {
            keeper.discard(target)
            run(starter.fresh(target, prefs))
        }
    }

    /**
     * Looks for an unfinished session of this scope: offers it if there is one that can still be continued, otherwise starts a
     * new session.
     */
    private suspend fun open() {
        val unfinished = starter.unfinished(target) ?: return run(starter.fresh(target, prefs))
        offered = unfinished
        _ui.update {
            it.copy(
                loading = false,
                offering = true,
                stepsLeft = unfinished.planner.stepsLeft,
                completedWords = unfinished.progress.completedWords,
                totalWords = unfinished.progress.totalWords,
            )
        }
    }

    /**
     * Runs a session: its settings decide the answer side and the way steps are prepared, and its counters replace what the screen
     * has. The screen keeps its loading spinner until the first step is ready, because preparing a step can take a moment (a Pair
     * board is filled up from the database) and a step cannot be drawn without its cards.
     *
     * @param start the session to run.
     */
    private suspend fun run(start: SessionStart) {
        session = start.settings
        presenter = StepPresenter(study, session.direction, random, session.pairSize)
        start.cards.forEach { cards[it.id] = it }
        planner = start.planner
        timer = SessionTimer(clock, laterSteps(start.planner), prefs.timeLeftAfter)
        val progress = start.progress
        _ui.update {
            it.copy(
                scopeName = scopeName,
                showTimeLeft = prefs.showTimeLeft,
                direction = session.direction,
                stepsDone = progress.stepsDone,
                correct = progress.correct,
                wrong = progress.wrong,
                completedWords = progress.completedWords,
                totalWords = progress.totalWords,
            )
        }
        show(withContext(NonCancellable) { pick(progress.stepsDone) }, progress.stepsDone)
    }

    /**
     * The steps of the sessions that come after this one and are still needed to complete everything in the scope.
     *
     * @param plan the running session; the words that have not finished yet each get one of their sessions done by it.
     * @return the steps per mode, counted per card.
     */
    private suspend fun laterSteps(plan: SessionPlanner): Map<StudyMode, Int> {
        val lacking = study.remainingSessions(deckId, chapterId, partId, session.sessionsToComplete)
        return SessionPlanner.stepsOfSessions(session.modes, session.rounds, (lacking - plan.activeCards).coerceAtLeast(0))
    }

    /**
     * Saves the words that just finished, counts the step and moves to the next one. Saving and choosing the next step (which
     * writes the state of the session) cannot be cancelled in the middle: leaving the screen then would let a word be saved to
     * the database but still be planned in the kept session. The count and the next step reach the screen together.
     *
     * @param timed false when the step was skipped and says nothing about the learner's pace.
     */
    private fun proceed(timed: Boolean = true) {
        val answered = step
        if (timed && answered != null) timer?.stepDone(answered)
        viewModelScope.launch {
            val done = _ui.value.stepsDone + 1
            val next = withContext(NonCancellable) {
                save()
                pick(done)
            }
            show(next, done)
        }
    }

    /** Saves the result of every word that finished its plan since the last call. */
    private suspend fun save() {
        val outcomes = planner?.drainFinished().orEmpty()
        for (outcome in outcomes) {
            val card = cards[outcome.cardId] ?: continue
            val grade = SessionGrading.gradeFor(outcome.accuracy, outcome.knew)
            cards[outcome.cardId] = study.finishSession(card, grade, session.spaceSessions)
        }
        if (outcomes.isNotEmpty()) _ui.update { it.copy(completedWords = it.completedWords + outcomes.size) }
    }

    /**
     * Chooses the next step and keeps the state of the session, or forgets the session when it is over.
     *
     * @param done the number of steps finished once the next step is shown.
     * @return the step to show, or null when every word has finished.
     */
    private suspend fun pick(done: Int): SessionPlanner.Step? {
        val plan = planner ?: return null
        val next = plan.next()
        step = next
        if (next == null) keeper.discard(target) else keeper.keep(target, session, progress(done), plan.snapshot())
        return next
    }

    /**
     * The counters on screen, as they are kept with the session.
     *
     * @param done the number of steps finished.
     * @return the counters.
     */
    private fun progress(done: Int): SessionProgress =
        _ui.value.let { SessionProgress(done, it.correct, it.wrong, it.completedWords, it.totalWords) }

    /**
     * Shows a step, or the summary when the session is over.
     *
     * @param next the step chosen by [pick], or null.
     * @param done the number of steps finished.
     */
    private suspend fun show(next: SessionPlanner.Step?, done: Int) {
        val plan = planner ?: return
        if (next == null) return finish(done)
        val shown = presenter.present(next, cards)
        timer?.stepShown()
        _ui.update {
            it.copy(
                loading = false,
                mode = next.mode,
                exposure = next.mode == StudyMode.LEARN && plan.modes.size > 1,
                current = shown.cards,
                decoyIds = shown.decoyIds,
                options = shown.options,
                stepsDone = done,
                stepsLeft = plan.stepsLeft,
                timeLeft = timer?.remaining(plan.stepsLeftByMode),
            )
        }
    }

    /**
     * Shows the summary: every word has finished.
     *
     * @param done the number of steps finished.
     */
    private fun finish(done: Int) {
        _ui.update {
            it.copy(
                loading = false,
                current = emptyList(),
                options = emptyList(),
                stepsDone = done,
                stepsLeft = 0,
                timeLeft = null,
                finished = true,
            )
        }
    }
}
