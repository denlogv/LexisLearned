package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.epub.EpubChapter
import java.io.IOException
import kotlinx.serialization.SerializationException

/**
 * The sections of a book being turned into chapters of one deck, and what became of each: a section is handled once its cards
 * were fetched, so the ones that failed, were interrupted or were never reached stay [unfinished] and can be done in a later run.
 *
 * @param header the deck's details without chapters.
 * @param sections the sections to process, in reading order.
 * @param assembler collects the cards of the finished sections.
 * @param systemPrompt the system prompt with the card rules (see [Prompts.build]).
 */
class GenerationRun internal constructor(
    private val header: Deck,
    private val sections: List<EpubChapter>,
    private val assembler: DeckAssembler,
    private val systemPrompt: String,
) {
    private val failedSections = ArrayList<String>()
    private val handled = HashSet<Int>()
    private var stored = 0

    @Volatile private var inSection = false

    /**
     * Whether a section is being handled right now: its request is in flight, or its chapter is being stored and saved. A pause that
     * waits for the section is meaningful only then; at any other time there is nothing to wait for.
     */
    val sectionInFlight: Boolean get() = inSection

    /** Descriptions of the sections that failed and were skipped. */
    val failed: List<String> get() = failedSections.toList()

    /** Indexes of the sections whose cards were not fetched: the failed ones, the interrupted one and those not reached. */
    val unfinished: Set<Int> get() = sections.map { it.index }.filter { it !in handled }.toSet()

    /**
     * Processes the sections in order until all are done or [GenerationOptions.isCancelled] says to stop. A section whose request
     * fails is recorded and skipped; one that is interrupted by cancellation counts as not done.
     *
     * @param llm the model to ask.
     * @param options card counts, progress, cancellation and per-chapter callbacks. Progress counts the sections whose chapter was added
     * to the deck, not the ones that were tried: a section that failed or had nothing new does not advance it.
     * @throws LlmException if the provider rejects the API key.
     */
    suspend fun process(llm: LlmClient, options: GenerationOptions) {
        val fetcher = ChapterCardFetcher(llm, systemPrompt)
        for (section in sections) {
            if (options.isCancelled()) return
            inSection = true
            try {
                handle(fetcher, section, options)
            } finally {
                inSection = false
            }
        }
        options.onProgress(stored, sections.size, assembler.cardCount, "Done")
    }

    /**
     * Fetches one section's cards and, if there are new ones, stores them as a chapter.
     *
     * @param fetcher asks the model for the cards.
     * @param section the section.
     * @param options card counts, progress and per-chapter callbacks.
     * @throws LlmException if the provider rejects the API key.
     */
    private suspend fun handle(fetcher: ChapterCardFetcher, section: EpubChapter, options: GenerationOptions) {
        options.onProgress(stored, sections.size, assembler.cardCount, section.title)
        val wanted = (section.words * options.cardsPer1000Words / 1000).coerceIn(options.minCards, options.maxCards)
        val reply = fetchOrRecord(fetcher, section, wanted)
        if (reply != null) handled += section.index
        val chapter = reply?.let { assembler.add(section, it) }
        if (chapter != null) {
            options.onChapter(header, chapter)
            stored++
        }
        options.onSectionDone()
    }

    /**
     * The deck as far as it is generated.
     *
     * @return the deck with all chapters of the finished sections, and the sections that failed.
     */
    fun result(): GenerationResult =
        GenerationResult(assembler.build(header.title, header.nativeTitle, header.frontLang, header.backLang), failed)

    /**
     * Fetches a section's cards; any failure except cancellation and a rejected API key is recorded instead of thrown.
     *
     * @param fetcher asks the model for the cards.
     * @param section the section.
     * @param wanted roughly how many cards are wanted.
     * @return the model's reply, or null if the section failed.
     * @throws LlmException if the provider rejects the API key.
     */
    private suspend fun fetchOrRecord(fetcher: ChapterCardFetcher, section: EpubChapter, wanted: Int): ChapterResponse? = try {
        fetcher.fetch(header.title, section.title, section.text, wanted)
    } catch (e: LlmException) {
        if (e.code in AUTH_ERRORS) throw e
        failedSections += "${section.title} (${e.message})"
        null
    } catch (e: SerializationException) {
        failedSections += "${section.title} (${e.message})"
        null
    } catch (e: IOException) {
        failedSections += "${section.title} (${e.message})"
        null
    }

    private companion object {
        val AUTH_ERRORS = setOf(401, 403)
    }
}
