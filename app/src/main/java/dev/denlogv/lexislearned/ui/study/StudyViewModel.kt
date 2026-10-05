package dev.denlogv.lexislearned.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.SessionGrading
import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode
import dev.denlogv.lexislearned.ui.Routes
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A study session: every word runs through every selected mode (repeated for the configured number of rounds) before the
 * session is done. The order comes from [SessionPlanner]; this class shows the steps and saves each word's result as soon as
 * that word has finished its whole plan.
 *
 * The navigation arguments `deckId`, `chapterId` and `partId` come from [handle]; [Routes.NONE] stands for "all".
 *
 * @param study the words of a session and where its results are saved.
 * @param settings the user's settings; they are read once when the session starts.
 * @param handle the saved state holding the navigation arguments.
 * @param random source of randomness for the order of steps and answer choices.
 */
class StudyViewModel(
    private val study: StudyRepository,
    settings: Settings,
    handle: SavedStateHandle,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val deckId: Long = handle["deckId"]!!
    private val chapterId: Long? = handle.get<Long>("chapterId")?.takeIf { it >= 0 }
    private val partId: Long? = handle.get<Long>("partId")?.takeIf { it >= 0 }
    private val prefs: Prefs = settings.prefs.value
    private val presenter = StepPresenter(study, prefs.direction, random, prefs.pairSize)
    private val cards = HashMap<Long, CardEntity>()
    private var planner: SessionPlanner? = null
    private var step: SessionPlanner.Step? = null
    private val _ui = MutableStateFlow(StudyUi())

    /** Which side of a card is the question in this session. */
    val direction: Direction = prefs.direction

    /** What the screen shows. */
    val ui: StateFlow<StudyUi> = _ui

    init {
        viewModelScope.launch { start() }
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
        proceed()
    }

    /** Loads the session's words, plans the session and shows its first step. */
    private suspend fun start() {
        val list = study.sessionCards(deckId, chapterId, partId, prefs.sessionsToComplete, prefs.sessionSize, prefs.newPerSession)
        list.forEach { cards[it.id] = it }
        planner = SessionPlanner(list.map { it.id }, prefs.modes, prefs.rounds, random, prefs.pairSize)
        _ui.value = StudyUi(loading = false, totalWords = list.size)
        advance()
    }

    /** Saves the words that just finished, counts the step and shows the next one. */
    private fun proceed() {
        viewModelScope.launch {
            save()
            _ui.update { it.copy(stepsDone = it.stepsDone + 1) }
            advance()
        }
    }

    /** Saves the result of every word that finished its plan since the last call. */
    private suspend fun save() {
        val outcomes = planner?.drainFinished().orEmpty()
        for (outcome in outcomes) {
            val card = cards[outcome.cardId] ?: continue
            val grade = SessionGrading.gradeFor(outcome.accuracy, outcome.knew)
            cards[outcome.cardId] = study.finishSession(card, grade, prefs.spaceSessions)
        }
        if (outcomes.isNotEmpty()) _ui.update { it.copy(completedWords = it.completedWords + outcomes.size) }
    }

    /** Shows the next step, or the summary when the session is over. */
    private suspend fun advance() {
        val plan = planner ?: return
        val next = plan.next()
        step = next
        if (next == null) {
            _ui.update { it.copy(current = emptyList(), options = emptyList(), stepsLeft = 0, finished = true) }
            return
        }
        val shown = presenter.present(next, cards)
        _ui.update {
            it.copy(
                mode = next.mode,
                exposure = next.mode == StudyMode.LEARN && plan.modes.size > 1,
                current = shown.cards,
                decoyIds = shown.decoyIds,
                options = shown.options,
                stepsLeft = plan.stepsLeft,
            )
        }
    }
}
