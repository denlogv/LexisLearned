package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.epub.EpubBook
import kotlin.coroutines.cancellation.CancellationException

/**
 * One generation of a deck from a book, storing each chapter as it is finished and ending in a [GenState].
 *
 * @param book the book.
 * @param prefs the settings in effect when generation started.
 * @param storage where the deck is stored.
 * @param options which sections to use, card counts, progress and cancellation callbacks; [GenerationOptions.isCancelled] also
 * tells a stop requested by the user from any other cancellation.
 * @param onStored called after each chapter was stored, with the deck's id and the number of cards stored so far.
 */
internal class GenerationJob(
    private val book: EpubBook,
    private val prefs: Prefs,
    storage: DeckStorage,
    private val options: GenerationOptions,
    private val onStored: (deckId: Long, cards: Int) -> Unit,
) {
    private val writer = DeckWriter(storage)
    private var run: GenerationRun? = null

    /**
     * Processes the selected sections and stores their chapters as they are finished.
     *
     * @param llm the model to ask.
     * @return where things stand afterwards, see [outcome]; stopping on request gives the deck so far, or the review again if
     * nothing was stored.
     * @throws CancellationException if the work was cancelled for a reason other than the user's stop.
     */
    @Suppress("TooGenericExceptionCaught") // Top of a background job: whatever goes wrong must reach the user, not crash the app.
    suspend fun execute(llm: LlmClient): GenState {
        val streaming = options.copy(onChapter = { deck, chapter -> onStored(writer.add(deck, chapter), writer.cards) })
        return try {
            val current = generator(llm).start(book, streaming).also { run = it }
            current.process(llm, streaming)
            outcome(emptyList())
        } catch (e: CancellationException) {
            if (!options.isCancelled()) throw e
            if (writer.deckId == null) GenState.Ready(book) else outcome(emptyList())
        } catch (e: Exception) {
            outcome(listOf(e.message ?: "Generation failed"))
        }
    }

    /**
     * Creates the generator for this job's settings.
     *
     * @param llm the model to ask.
     * @return the generator.
     */
    private fun generator(llm: LlmClient) =
        CardGenerator(llm, prefs.targetLang, prefs.level, prefs.customRules[prefs.level], prefs.extraInstructions)

    /**
     * The state after the work: the deck if one exists, otherwise a failure.
     *
     * @param extraFailures problems that ended the work, added to the sections that failed.
     * @return [GenState.Finished] or [GenState.Failed].
     */
    private fun outcome(extraFailures: List<String>): GenState {
        val failed = run?.failed.orEmpty() + extraFailures
        val id = writer.deckId ?: return GenState.Failed(failed.firstOrNull() ?: "No cards were generated.", book)
        return GenState.Finished(id, writer.cards, failed)
    }
}
