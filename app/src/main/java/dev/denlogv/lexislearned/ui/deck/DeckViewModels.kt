package dev.denlogv.lexislearned.ui.deck

import androidx.lifecycle.viewModelScope
import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.ChapterEntity
import dev.denlogv.lexislearned.data.ChapterSummary
import dev.denlogv.lexislearned.data.DeckEntity
import dev.denlogv.lexislearned.data.DeckRepository
import dev.denlogv.lexislearned.data.PartEntity
import dev.denlogv.lexislearned.data.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The state of the book screen.
 *
 * @param repo access to stored decks.
 * @param settings the user's settings.
 * @property deckId the book being shown.
 */
class DeckViewModel(private val repo: DeckRepository, settings: Settings, val deckId: Long) : StudyScopeViewModel(settings) {
    /** The book; null until loaded. */
    val deck: StateFlow<DeckEntity?> = repo.deck(deckId).asState(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val chapterFlow: Flow<List<ChapterSummary>> = needed.flatMapLatest { repo.chapters(deckId, it) }

    /** All chapters of the book with their progress. */
    val chapters: StateFlow<List<ChapterSummary>> = chapterFlow.asState(emptyList())

    /** The rows of the book screen: parts and loose chapters in reading order. */
    val entries: StateFlow<List<DeckEntry>> = combine(repo.parts(deckId), chapterFlow, ::buildEntries).asState(emptyList())

    /**
     * Makes every word of the book new again.
     *
     * @return the running job.
     */
    fun resetBook(): Job = viewModelScope.launch { repo.resetProgress(deckId) }
}

/**
 * The state of the part screen.
 *
 * @param repo access to stored decks.
 * @param settings the user's settings.
 * @property partId the part being shown.
 */
class PartViewModel(private val repo: DeckRepository, settings: Settings, val partId: Long) : StudyScopeViewModel(settings) {
    /** The part; null until loaded. */
    val part: StateFlow<PartEntity?> = repo.part(partId).asState(null)

    /** The book the part belongs to. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val deck: StateFlow<DeckEntity?> = part.flatMapLatest { p -> if (p == null) flowOf(null) else repo.deck(p.deckId) }.asState(null)

    /** The chapters of the part with their progress. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterSummary>> = combine(part, needed) { p, n -> p to n }.flatMapLatest { (p, n) ->
        if (p == null) flowOf(emptyList()) else repo.chapters(p.deckId, n).map { list -> list.filter { it.partId == partId } }
    }.asState(emptyList())

    /**
     * Makes every word of the part new again.
     *
     * @return the running job.
     */
    fun resetPart(): Job = viewModelScope.launch { repo.resetPart(partId) }
}

/**
 * The state of the chapter screen.
 *
 * @param repo access to stored decks.
 * @param settings the user's settings.
 * @property chapterId the chapter being shown.
 */
class ChapterViewModel(private val repo: DeckRepository, settings: Settings, val chapterId: Long) : StudyScopeViewModel(settings) {
    /** The chapter; null until loaded. */
    val chapter: StateFlow<ChapterEntity?> = repo.chapter(chapterId).asState(null)

    /** The chapter's progress; null until loaded. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: StateFlow<ChapterSummary?> = needed.flatMapLatest { repo.chapterSummary(chapterId, it) }.asState(null)

    /** The chapter's words. */
    val cards: StateFlow<List<CardEntity>> = repo.cardsOfChapter(chapterId).asState(emptyList())

    /** The book the chapter belongs to. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val deck: StateFlow<DeckEntity?> = chapter.flatMapLatest { c -> if (c == null) flowOf(null) else repo.deck(c.deckId) }.asState(null)

    /**
     * Makes every word of the chapter new again.
     *
     * @return the running job.
     */
    fun resetChapter(): Job = viewModelScope.launch { repo.resetChapter(chapterId) }

    /**
     * Makes one word new again.
     *
     * @param id the word.
     * @return the running job.
     */
    fun resetCard(id: Long): Job = viewModelScope.launch { repo.resetCard(id) }
}
