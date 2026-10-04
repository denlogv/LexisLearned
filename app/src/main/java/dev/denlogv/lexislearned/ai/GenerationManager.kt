package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.epub.EpubBook
import dev.denlogv.lexislearned.epub.EpubReader
import java.io.ByteArrayInputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 * @param scope where background work runs.
 * @param storage where finished decks are imported.
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

    @Volatile private var cancelled = false

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
     * Starts generating the deck. Fails at once if no API key is saved.
     *
     * @param book the reviewed book.
     * @param selected indexes of the sections to use.
     * @param sourceLang the book's language code.
     * @param cardsPer1000Words how many cards to ask for per thousand words.
     */
    fun start(book: EpubBook, selected: Set<Int>, sourceLang: String, cardsPer1000Words: Int) {
        val prefs = settings.prefs.value
        val key = settings.apiKey()
        if (key.isNullOrBlank() || !prefs.ready) {
            _state.value = GenState.Failed(Providers.missingSetting(prefs), book)
            return
        }
        cancelled = false
        _state.value = GenState.Running(0, selected.size, 0, "Starting…")
        val options = GenerationOptions(
            selected = selected,
            sourceLang = sourceLang,
            cardsPer1000Words = cardsPer1000Words,
            onProgress = { done, total, cards, msg -> _state.value = GenState.Running(done, total, cards, msg) },
            isCancelled = { cancelled },
        )
        scope.launch { _state.value = generate(book, options, prefs, clientFactory(prefs, key)) }
    }

    /**
     * Moves to [GenState.Failed], for problems that happen before generation, such as an unreadable file.
     *
     * @param message what went wrong, phrased for the user.
     */
    fun fail(message: String) {
        _state.value = GenState.Failed(message)
    }

    /** Stops after the current section; sections finished so far are still imported. */
    fun cancel() {
        cancelled = true
    }

    /** Returns to [GenState.Idle] unless generation is running. */
    fun reset() {
        if (_state.value !is GenState.Running) _state.value = GenState.Idle
    }

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

    /**
     * Generates the deck and imports it.
     *
     * @param book the book.
     * @param options which sections to use, progress and cancellation callbacks.
     * @param prefs the settings in effect when generation started.
     * @param llm the model to ask.
     * @return [GenState.Finished], or [GenState.Failed] if nothing could be generated or something went wrong.
     */
    @Suppress("TooGenericExceptionCaught") // Top of a background job: whatever goes wrong must reach the user, not crash the app.
    private suspend fun generate(book: EpubBook, options: GenerationOptions, prefs: Prefs, llm: LlmClient): GenState = try {
        val generator = CardGenerator(llm, prefs.targetLang, prefs.level, prefs.customRules[prefs.level], prefs.extraInstructions)
        val result = generator.generate(book, options)
        if (result.deck.chapters.isEmpty()) {
            GenState.Failed(result.failedChapters.firstOrNull() ?: "No cards were generated.", book)
        } else {
            GenState.Finished(storage.import(result.deck), result.deck.cardCount, result.failedChapters)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        GenState.Failed(e.message ?: "Generation failed", book)
    }
}
