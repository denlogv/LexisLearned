package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.epub.EpubBook
import dev.denlogv.lexislearned.epub.EpubReader
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Creates a client for the provider in the settings.
 *
 * The arguments are the settings (provider, model and custom address) and the API key.
 */
typealias ClientFactory = (Prefs, String) -> LlmClient

/**
 * Runs "EPUB to deck" in the application scope so it survives screen changes, and publishes its progress as [GenState].
 *
 * The deck is stored while it is generated: the book appears in the library with its first chapter, and every further chapter
 * is added as soon as it is ready. Stopping therefore loses nothing that was finished, and the rest of the book can be added to the
 * same deck later (see [start]).
 *
 * @param scope where background work runs.
 * @param storage where the deck is stored.
 * @param settings provider, key, level and prompt settings.
 * @param clientFactory creates the LLM client; tests replace it with a fake.
 */
class GenerationManager(
    private val scope: CoroutineScope,
    private val storage: DeckStorage,
    private val settings: Settings,
    private val clientFactory: ClientFactory = Providers::defaultClient,
) {
    private val _state = MutableStateFlow<GenState>(GenState.Idle)

    /** The current stage. */
    val state: StateFlow<GenState> = _state

    @Volatile private var job: Job? = null

    @Volatile private var stopRequested = false

    /**
     * Reads an EPUB in the background and moves to [GenState.Ready], or to [GenState.Failed] if it cannot be read.
     *
     * @param bytes the EPUB file contents.
     */
    fun load(bytes: ByteArray) {
        _state.value = GenState.Loading
        scope.launch { _state.value = readBook(bytes) }
    }

    /**
     * Starts generating the deck. Fails at once if the provider is not set up.
     *
     * @param book the reviewed book.
     * @param selected indexes of the sections to use.
     * @param sourceLang the book's language code.
     * @param cardsPer1000Words how many cards to ask for per thousand words.
     * @param continuation the deck to add the chapters to, when the rest of a book is added to a deck that was stopped early; null
     * to create a new deck.
     */
    fun start(book: EpubBook, selected: Set<Int>, sourceLang: String, cardsPer1000Words: Int, continuation: GenState.Continuation? = null) {
        val prefs = settings.prefs.value
        val llm = clientOrFail(prefs, book) ?: return
        val options = GenerationOptions(
            selected = selected,
            sourceLang = sourceLang,
            cardsPer1000Words = cardsPer1000Words,
            onProgress = { done, total, cards, msg -> publish { it.copy(done = done, total = total, cards = cards, message = msg) } },
            isCancelled = { stopRequested },
        )
        val job =
            GenerationJob(book, prefs, storage, options, continuation) { id, cards -> publish { it.copy(deckId = id, cards = cards) } }
        _state.value = GenState.Running(0, selected.size, 0, "Starting…", book.title, continuation?.deckId)
        launch(job, llm)
    }

    /**
     * Moves to [GenState.Failed], for problems that happen before generation, such as an unreadable file.
     *
     * @param message what went wrong, phrased for the user.
     */
    fun fail(message: String) {
        _state.value = GenState.Failed(message)
    }

    /**
     * Stops generation at once, also abandoning the request in flight. Chapters that were finished are already in the library:
     * the state becomes [GenState.Finished] for them, or [GenState.Ready] if there were none.
     */
    fun cancel() {
        stopRequested = true
        job?.cancel()
    }

    /** Returns to [GenState.Idle] unless generation is running. */
    fun reset() {
        if (_state.value !is GenState.Running) _state.value = GenState.Idle
    }

    /**
     * Creates the client, or moves to [GenState.Failed] if the provider is not set up.
     *
     * @param prefs the current settings.
     * @param book the book, so the user can retry from the failure screen; null if there is none to offer.
     * @return the client, or null after failing.
     */
    private fun clientOrFail(prefs: Prefs, book: EpubBook?): LlmClient? {
        val key = settings.apiKey()
        if (key.isNullOrBlank() || !prefs.ready) {
            _state.value = GenState.Failed(Providers.missingSetting(prefs), book)
            return null
        }
        return clientFactory(prefs, key)
    }

    /**
     * Runs a generation in the background and publishes where it ends up.
     *
     * @param generation the generation to run.
     * @param llm the model to ask.
     */
    private fun launch(generation: GenerationJob, llm: LlmClient) {
        stopRequested = false
        val launched = scope.launch(start = CoroutineStart.LAZY) { _state.value = generation.execute(llm) }
        job = launched
        launched.start()
    }

    /**
     * Changes the running state, if there is one; a late update never overwrites the result.
     *
     * @param change computes the new running state from the current one.
     */
    private fun publish(change: (GenState.Running) -> GenState.Running) = _state.update { (it as? GenState.Running)?.let(change) ?: it }

    /**
     * Reads the book.
     *
     * @param bytes the EPUB file contents.
     * @return [GenState.Ready], or [GenState.Failed] with the reason.
     */
    @Suppress("TooGenericExceptionCaught") // Top of a background job: whatever goes wrong must reach the user, not crash the app.
    private fun readBook(bytes: ByteArray): GenState = try {
        GenState.Ready(EpubReader.read(ByteArrayInputStream(bytes)))
    } catch (e: Exception) {
        GenState.Failed(e.message ?: "Could not read the EPUB")
    }
}
