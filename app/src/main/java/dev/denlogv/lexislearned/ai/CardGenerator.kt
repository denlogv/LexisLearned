package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.domain.PartRef
import dev.denlogv.lexislearned.epub.EpubBook
import dev.denlogv.lexislearned.epub.EpubChapter
import java.io.IOException
import java.util.UUID
import kotlinx.serialization.SerializationException

/**
 * Turns a book into pre-reading vocabulary cards, one section at a time, above the learner's CEFR level.
 *
 * @param llm the model to ask.
 * @param targetLang the language code the cards are translated into.
 * @param level the learner's level; cards cover vocabulary above it.
 * @param customRules the user's own card rules for this level, or null for the built-in ones.
 * @param extraInstructions extra instructions from the user, appended to the prompt.
 */
class CardGenerator(
    private val llm: LlmClient,
    private val targetLang: String,
    private val level: CefrLevel = CefrLevel.B1,
    private val customRules: String? = null,
    private val extraInstructions: String = "",
) {
    /**
     * Generates a deck. A section whose request fails is skipped and reported; an authentication error aborts everything.
     *
     * @param book the book.
     * @param options which sections to process and how many cards to ask for.
     * @return the deck and the descriptions of sections that failed.
     * @throws LlmException if the provider rejects the API key.
     */
    suspend fun generate(book: EpubBook, options: GenerationOptions = GenerationOptions()): GenerationResult {
        val sourceLang = options.sourceLang.ifBlank { book.language }.take(2).lowercase().ifBlank { "en" }
        val sections = book.chapters.filter { it.index in (options.selected ?: book.defaultSelection) }
        val titles = TitleTranslator(llm, Languages.nameOf(targetLang))
            .translate(book.title, sections.mapNotNull { it.part }.distinct())
        val assembler = DeckAssembler(partRefsFor(sections, titles))
        val fetcher =
            ChapterCardFetcher(
                llm,
                Prompts.build(Languages.nameOf(sourceLang), Languages.nameOf(targetLang), level, customRules, extraInstructions),
            )
        val failed = processSections(book.title, sections, options, fetcher, assembler)
        options.onProgress(sections.size, sections.size, assembler.cardCount, "Done")
        return GenerationResult(assembler.build(book.title, titles.book, sourceLang, targetLang), failed)
    }

    /**
     * Creates the parts used by the selected sections, with translated titles where available.
     *
     * @param sections the selected sections.
     * @param titles the translated titles.
     * @return the parts by original title.
     */
    private fun partRefsFor(sections: List<EpubChapter>, titles: TitleTranslator.Titles): Map<String, PartRef> =
        sections.mapNotNull { it.part }.distinct().associateWith { title ->
            PartRef(UUID.randomUUID().toString(), title, titles.parts[title]?.takeIf { it.isNotBlank() })
        }

    /**
     * Fetches and assembles every section, reporting progress and honouring cancellation.
     *
     * @param bookTitle the book's title.
     * @param sections the sections to process.
     * @param options progress and cancellation callbacks and card counts.
     * @param fetcher asks the model for a section's cards.
     * @param assembler collects the results.
     * @return descriptions of the sections that failed.
     */
    private suspend fun processSections(
        bookTitle: String,
        sections: List<EpubChapter>,
        options: GenerationOptions,
        fetcher: ChapterCardFetcher,
        assembler: DeckAssembler,
    ): List<String> {
        val failed = ArrayList<String>()
        for ((i, section) in sections.withIndex()) {
            if (options.isCancelled()) break
            options.onProgress(i, sections.size, assembler.cardCount, section.title)
            val wanted = (section.words * options.cardsPer1000Words / 1000).coerceIn(options.minCards, options.maxCards)
            fetchOrRecord(fetcher, bookTitle, section, wanted, failed)?.let { assembler.add(section, it) }
        }
        return failed
    }

    /**
     * Fetches a section's cards; any failure except cancellation and a rejected API key is recorded instead of thrown.
     *
     * @param fetcher asks the model for the cards.
     * @param bookTitle the book's title.
     * @param section the section.
     * @param wanted roughly how many cards are wanted.
     * @param failed receives a description if the section fails.
     * @return the model's reply, or null if the section failed.
     * @throws LlmException if the provider rejects the API key.
     */
    private suspend fun fetchOrRecord(
        fetcher: ChapterCardFetcher,
        bookTitle: String,
        section: EpubChapter,
        wanted: Int,
        failed: MutableList<String>,
    ): ChapterResponse? = try {
        fetcher.fetch(bookTitle, section.title, section.text, wanted)
    } catch (e: LlmException) {
        if (e.code in AUTH_ERRORS) throw e
        failed += "${section.title} (${e.message})"
        null
    } catch (e: SerializationException) {
        failed += "${section.title} (${e.message})"
        null
    } catch (e: IOException) {
        failed += "${section.title} (${e.message})"
        null
    }

    private companion object {
        val AUTH_ERRORS = setOf(401, 403)
    }
}
