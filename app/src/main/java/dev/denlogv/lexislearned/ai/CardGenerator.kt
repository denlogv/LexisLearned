package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.domain.PartRef
import dev.denlogv.lexislearned.epub.EpubBook
import java.util.UUID

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
        val run = start(book, options)
        run.process(llm, options)
        return run.result()
    }

    /**
     * Prepares a run without processing any section yet: translates the titles and sets up the deck.
     *
     * @param book the book.
     * @param options which sections to process and the book's language.
     * @return the run.
     * @throws LlmException if the provider rejects the API key.
     */
    suspend fun start(book: EpubBook, options: GenerationOptions): GenerationRun {
        val sourceLang = options.sourceLang.ifBlank { book.language }.take(2).lowercase().ifBlank { "en" }
        val sections = book.chapters.filter { it.index in (options.selected ?: book.defaultSelection) }
        val parts = sections.mapNotNull { it.part }.distinct()
        val titles = TitleTranslator(llm, Languages.nameOf(targetLang)).translate(book.title, parts)
        val assembler = DeckAssembler(partRefsFor(parts, titles))
        val prompt = Prompts.build(Languages.nameOf(sourceLang), Languages.nameOf(targetLang), level, customRules, extraInstructions)
        return GenerationRun(assembler.header(book.title, titles.book, sourceLang, targetLang), sections, assembler, prompt)
    }

    /**
     * Creates new parts, with translated titles where available.
     *
     * @param partTitles the original titles of the parts to create.
     * @param titles the translated titles.
     * @return the parts by original title.
     */
    private fun partRefsFor(partTitles: List<String>, titles: TitleTranslator.Titles): Map<String, PartRef> =
        partTitles.associateWith { title ->
            PartRef(UUID.randomUUID().toString(), title, titles.parts[title]?.takeIf { it.isNotBlank() })
        }
}
