package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.domain.Deck
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
        val run = start(book, options, null)
        run.process(llm, options)
        return run.result()
    }

    /**
     * Prepares a run without processing any section yet: translates the titles and sets up the deck.
     *
     * @param book the book.
     * @param options which sections to process and the book's language.
     * @param base a deck made from this book earlier, to add the new chapters to: its titles, languages and parts are reused and
     * its words are not repeated; null for a new deck.
     * @return the run.
     * @throws LlmException if the provider rejects the API key.
     */
    suspend fun start(book: EpubBook, options: GenerationOptions, base: Deck?): GenerationRun {
        val sourceLang = base?.frontLang ?: options.sourceLang.ifBlank { book.language }.take(2).lowercase().ifBlank { "en" }
        val target = base?.backLang ?: targetLang
        val sections = book.chapters.filter { it.index in (options.selected ?: book.defaultSelection) }
        val known = base?.chapters?.mapNotNull { it.part }?.associateBy { it.title }.orEmpty()
        val newParts = sections.mapNotNull { it.part }.distinct().filter { it !in known }
        val titles = if (base != null && newParts.isEmpty()) {
            TitleTranslator.Titles(base.nativeTitle, emptyMap())
        } else {
            TitleTranslator(llm, Languages.nameOf(target)).translate(book.title, newParts)
        }
        val assembler = DeckAssembler(known + partRefsFor(newParts, titles), base)
        val prompt = Prompts.build(Languages.nameOf(sourceLang), Languages.nameOf(target), level, customRules, extraInstructions)
        val header = assembler.header(base?.title ?: book.title, base?.nativeTitle ?: titles.book, sourceLang, target)
        return GenerationRun(header, sections, assembler, prompt)
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
