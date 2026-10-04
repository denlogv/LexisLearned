package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.epub.EpubChapter
import java.io.IOException
import kotlinx.serialization.SerializationException

/**
 * The sections of a book being turned into chapters of one deck, one after the other.
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
    private var stored = 0

    /** Descriptions of the sections that failed and were skipped. */
    val failed: List<String> get() = failedSections.toList()

    /**
     * Processes the sections in order until all are done or [GenerationOptions.isCancelled] says to stop. A section whose request
     * fails is recorded and skipped.
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
            options.onProgress(stored, sections.size, assembler.cardCount, section.title)
            val wanted = (section.words * options.cardsPer1000Words / 1000).coerceIn(options.minCards, options.maxCards)
            val chapter = fetchOrRecord(fetcher, section, wanted)?.let { assembler.add(section, it) }
            if (chapter != null) {
                options.onChapter(header, chapter)
                stored++
            }
        }
        options.onProgress(stored, sections.size, assembler.cardCount, "Done")
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
