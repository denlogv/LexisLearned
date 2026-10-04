package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.epub.EpubBook
import kotlin.coroutines.cancellation.CancellationException

/**
 * One run of generating a deck from a book, new or resumed. It stores each chapter as it is finished, keeps the [JobJournal] up to
 * date so the run can be resumed after a restart, and ends in a [GenState].
 *
 * @param book the book.
 * @param prefs the settings in effect when generation started.
 * @param storage where the deck is stored.
 * @param options which sections to use, card counts, progress and cancellation callbacks; [GenerationOptions.isCancelled] also
 * tells a pause requested by the user from any other cancellation.
 * @param continuation the deck to add the chapters to, or null to create a new deck.
 * @param journal where the progress is kept for a resume after a restart.
 * @param onStored called after each chapter was stored, with the deck's id and the number of cards stored so far.
 */
internal class GenerationJob(
    private val book: EpubBook,
    private val prefs: Prefs,
    private val storage: DeckStorage,
    private val options: GenerationOptions,
    private val continuation: GenState.Continuation?,
    private val journal: JobJournal,
    private val onStored: (deckId: Long, cards: Int) -> Unit,
) {
    private var writer = DeckWriter(storage)

    @Volatile private var run: GenerationRun? = null

    /** Whether a section is being worked on right now; false before the first one starts, while the titles are translated. */
    val sectionInFlight: Boolean get() = run?.sectionInFlight == true

    /**
     * Processes the selected sections and stores their chapters as they are finished.
     *
     * @param llm the model to ask.
     * @return where things stand afterwards, see [outcome]. A pause on request ends in [GenState.Paused] with the sections that
     * are left, as does a failure of some sections.
     * @throws CancellationException if the work was cancelled for a reason other than the user's pause.
     */
    @Suppress("TooGenericExceptionCaught") // Top of a background job: whatever goes wrong must reach the user, not crash the app.
    suspend fun execute(llm: LlmClient): GenState {
        val streaming = options.copy(
            onChapter = { deck, chapter -> onStored(writer.add(deck, chapter), writer.cards) },
            onSectionDone = { journal.update(record(unfinished(), failures(emptyList()))) },
        )
        val state = try {
            // The deck is opened first: the record must name it from the start, or a kill before the first chapter of a
            // resumed run would be restored without it and the rest would go into a second deck.
            val base = continuation?.deckId?.let { open(it) }
            journal.update(record(unfinished(), emptyList()))
            val current = generator(llm).start(book, streaming, base).also { run = it }
            current.process(llm, streaming)
            outcome(emptyList())
        } catch (e: CancellationException) {
            if (!options.isCancelled()) throw e
            outcome(emptyList())
        } catch (e: Exception) {
            outcome(listOf(e.message ?: "Generation failed"))
        }
        settle(state)
        return state
    }

    /**
     * Loads the deck that is continued and makes the writer add to it.
     *
     * @param deckId the deck to continue.
     * @return the stored deck.
     * @throws IllegalStateException if the deck no longer exists.
     */
    private suspend fun open(deckId: Long): Deck = storage.export(deckId).also { writer = DeckWriter(storage, deckId, it.cardCount) }

    /**
     * Creates the generator for this job's settings.
     *
     * @param llm the model to ask.
     * @return the generator.
     */
    private fun generator(llm: LlmClient) =
        CardGenerator(llm, prefs.targetLang, prefs.level, prefs.customRules[prefs.level], prefs.extraInstructions)

    /**
     * The sections that are not in the deck.
     *
     * @return those the run did not finish, or all chosen ones while the run has not started.
     */
    private fun unfinished(): Set<Int> = run?.unfinished ?: options.selected ?: book.defaultSelection

    /**
     * The problems so far.
     *
     * @param extra problems that did not come from a section, such as a rejected key.
     * @return the sections that failed, then [extra].
     */
    private fun failures(extra: List<String>): List<String> = run?.failed.orEmpty() + extra

    /**
     * The progress as it is kept for a resume.
     *
     * @param left the sections that are not in the deck.
     * @param failed the problems so far.
     * @return the record.
     */
    private fun record(left: Set<Int>, failed: List<String>) =
        JobRecord(writer.deckId, writer.cards, left, failed, options.sourceLang, options.cardsPer1000Words)

    /**
     * The state after the work.
     *
     * @param extra problems that ended the work, added to the sections that failed.
     * @return [GenState.Finished] if every chosen section is in the deck, [GenState.Paused] if some are left, otherwise
     * [GenState.Failed].
     */
    private fun outcome(extra: List<String>): GenState {
        val failed = failures(extra)
        val left = unfinished()
        val id = writer.deckId
        return when {
            left.isNotEmpty() -> GenState.Paused(id, writer.cards, book, left, failed)
            id != null -> GenState.Finished(id, writer.cards)
            else -> GenState.Failed(failed.firstOrNull() ?: "No cards were generated.", book)
        }
    }

    /**
     * Updates what is kept for a resume to match the result: the progress if the run can be resumed, nothing if it cannot.
     *
     * @param state the result of the run.
     */
    private suspend fun settle(state: GenState) {
        if (state is GenState.Paused) journal.update(record(state.remaining, state.failed)) else journal.clear()
    }
}
