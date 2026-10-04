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
 * is added as soon as it is ready. Pausing, a failed section or even the app being killed therefore loses nothing that was finished:
 * the book and the progress are kept on disk (see [JobStore]), [restore] brings them back as [GenState.Paused] on the next launch,
 * and [resume] does the sections that are left without paying for the finished ones again.
 *
 * @param scope where background work runs.
 * @param storage where the deck is stored.
 * @param settings provider, key, level and prompt settings.
 * @param store where an unfinished generation is kept between launches of the app.
 * @param clientFactory creates the LLM client; tests replace it with a fake.
 */
class GenerationManager(
    private val scope: CoroutineScope,
    private val storage: DeckStorage,
    private val settings: Settings,
    store: JobStore,
    private val clientFactory: ClientFactory = Providers::defaultClient,
) {
    private val _state = MutableStateFlow<GenState>(GenState.Idle)
    private val journal = JobJournal(store)

    /** The current stage. */
    val state: StateFlow<GenState> = _state

    @Volatile private var job: Job? = null

    @Volatile private var running: GenerationJob? = null

    @Volatile private var stopRequested = false

    @Volatile private var pauseAfterNextSection = false

    private var epub: ByteArray? = null
    private var choices = Choices("en", GenerationOptions().cardsPer1000Words)

    /**
     * What the user chose when generation was started, which a resume repeats.
     *
     * @property sourceLang the book's language code.
     * @property cardsPer1000Words how many cards to ask for per thousand words.
     */
    private class Choices(val sourceLang: String, val cardsPer1000Words: Int)

    /**
     * Reads an EPUB in the background and moves to [GenState.Ready], or to [GenState.Failed] if it cannot be read.
     *
     * @param bytes the EPUB file contents.
     */
    fun load(bytes: ByteArray) {
        _state.value = GenState.Loading
        epub = bytes
        scope.launch { _state.value = readBook(bytes) }
    }

    /**
     * Starts generating the deck. Fails at once if the provider is not set up.
     *
     * @param book the reviewed book.
     * @param selected indexes of the sections to use.
     * @param sourceLang the book's language code.
     * @param cardsPer1000Words how many cards to ask for per thousand words.
     * @param continuation the deck to add the chapters to, when the rest of a book is added to a deck that was paused; null to
     * create a new deck.
     */
    fun start(book: EpubBook, selected: Set<Int>, sourceLang: String, cardsPer1000Words: Int, continuation: GenState.Continuation? = null) {
        val prefs = settings.prefs.value
        val llm = clientOrReject(prefs, book) ?: return
        choices = Choices(sourceLang, cardsPer1000Words)
        val options = GenerationOptions(
            selected = selected,
            sourceLang = sourceLang,
            cardsPer1000Words = cardsPer1000Words,
            onProgress = { done, total, cards, msg ->
                if (pauseAfterNextSection) stopRequested = true // a section starts: this is the one the pause waits for
                publish { it.copy(done = done, total = total, cards = cards, message = msg) }
            },
            isCancelled = { stopRequested },
        )
        val job =
            GenerationJob(book, prefs, storage, options, continuation, journal) { id, cards ->
                publish { it.copy(deckId = id, cards = cards) }
            }
        _state.value = GenState.Running(0, selected.size, 0, "Starting…", book.title, continuation?.deckId)
        launch(job, llm)
    }

    /**
     * Does the sections that are left of a paused generation, adding them to the same deck with the choices made at the start.
     * Nothing happens unless the state is [GenState.Paused].
     */
    fun resume() {
        val paused = _state.value as? GenState.Paused ?: return
        val rest = GenState.Continuation(paused.deckId, paused.remaining)
        start(paused.book, paused.remaining, choices.sourceLang, choices.cardsPer1000Words, rest)
    }

    /**
     * Asks a running generation to pause once a section is done, because its request is paid for already; the state then becomes
     * [GenState.Paused]. The section is the one being worked on, or, when none is yet (for example while the titles are translated
     * before the first one), the next one to start: the button never ends a run without a section behind it, [pauseNow] is for that.
     * Nothing happens unless the state is [GenState.Running].
     */
    fun pauseAfterSection() {
        if (_state.value !is GenState.Running) return
        if (running?.sectionInFlight == true) stopRequested = true else pauseAfterNextSection = true
        publish { it.copy(pausing = true) }
    }

    /**
     * Pauses a running generation at once. The request in flight is given up and its section is done again on resume, so what that
     * request cost is lost; chapters that are finished stay. Nothing happens unless the state is [GenState.Running].
     */
    fun pauseNow() {
        if (_state.value !is GenState.Running) return
        stopRequested = true
        job?.cancel()
    }

    /**
     * Brings back a generation that the app left unfinished, as [GenState.Paused]. Call it when the app starts. Nothing is restored
     * if there is none, if it cannot be read, or if something else is going on already.
     */
    fun restore() {
        scope.launch {
            val stored = journal.restore() ?: return@launch
            val book = (readBook(stored.epub) as? GenState.Ready)?.book
            val record = stored.record
            if (book == null || record.remaining.isEmpty()) return@launch journal.clear()
            epub = stored.epub
            choices = Choices(record.sourceLang, record.cardsPer1000Words)
            _state.compareAndSet(GenState.Idle, GenState.Paused(record.deckId, record.cards, book, record.remaining, record.failed))
        }
    }

    /**
     * Moves to [GenState.Failed], for problems that happen before generation, such as an unreadable file.
     *
     * @param message what went wrong, phrased for the user.
     */
    fun fail(message: String) {
        _state.value = GenState.Failed(message)
    }

    /** Returns from [GenState.Failed] to the review of the book, which is still loaded, so the file need not be chosen again. */
    fun backToReview() {
        (_state.value as? GenState.Failed)?.book?.let { _state.value = GenState.Ready(it) }
    }

    /** Returns to [GenState.Idle] unless generation is running or paused; a pause is only ended by [discard]. */
    fun reset() {
        if (_state.value !is GenState.Running && _state.value !is GenState.Paused) _state.value = GenState.Idle
    }

    /** Gives up on the sections that are left of a paused generation: the deck stays in the library, the book is forgotten. */
    fun discard() {
        if (_state.value !is GenState.Paused) return
        _state.value = GenState.Idle
        // Undispatched: the journal queues the deletion before any write of a generation that starts next.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { journal.clear() }
    }

    /**
     * Creates the client. If the provider is not set up, a paused generation stays paused with the reason, and anything else fails.
     *
     * @param prefs the current settings.
     * @param book the book, so the user can go back to its review after a failure.
     * @return the client, or null after rejecting the start.
     */
    private fun clientOrReject(prefs: Prefs, book: EpubBook): LlmClient? {
        val key = settings.apiKey()
        if (!key.isNullOrBlank() && prefs.ready) return clientFactory(prefs, key)
        val message = Providers.missingSetting(prefs)
        _state.update { if (it is GenState.Paused) it.copy(failed = listOf(message)) else GenState.Failed(message, book) }
        return null
    }

    /**
     * Runs a generation in the background and publishes where it ends up.
     *
     * @param generation the generation to run.
     * @param llm the model to ask.
     */
    private fun launch(generation: GenerationJob, llm: LlmClient) {
        stopRequested = false
        pauseAfterNextSection = false
        running = generation
        val launched = scope.launch(start = CoroutineStart.LAZY) {
            epub?.let { journal.begin(it) }
            _state.value = generation.execute(llm)
        }
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
