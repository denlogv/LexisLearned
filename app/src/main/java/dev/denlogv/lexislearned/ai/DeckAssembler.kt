package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.Card
import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.domain.PartRef
import dev.denlogv.lexislearned.domain.Side
import dev.denlogv.lexislearned.epub.EpubChapter
import java.util.UUID

/**
 * Collects the generated cards into a deck: drops blank and duplicate words (the first chapter that has a word keeps it),
 * numbers the chapters and attaches them to their parts.
 *
 * @param partRefs the parts by title, for the chapters that belong to one.
 * @param base a deck that is continued: its words count as seen, its chapters come first in the numbering and the result, and
 * the result keeps its id; null for a new deck.
 */
internal class DeckAssembler(private val partRefs: Map<String, PartRef>, base: Deck? = null) {
    private val deckId = base?.id ?: UUID.randomUUID().toString()
    private val seen = HashSet<String>().apply {
        base?.chapters?.forEach { c -> c.cards.forEach { add(it.front.text.trim().lowercase()) } }
    }
    private val chapters = ArrayList(base?.chapters.orEmpty())

    /** Number of cards added so far. */
    val cardCount: Int get() = chapters.sumOf { it.cards.size }

    /**
     * Adds a chapter's cards. A chapter left without cards after filtering is not added.
     *
     * @param section the book section the cards were made for.
     * @param response the model's reply for the section.
     * @return the chapter that was added, or null if nothing was left of it.
     */
    fun add(section: EpubChapter, response: ChapterResponse): Chapter? {
        val cards = response.cards.filter { it.word.isNotBlank() && it.translation.isNotBlank() && seen.add(it.word.trim().lowercase()) }
        if (cards.isEmpty()) return null
        val number = "%02d".format(chapters.size + 1)
        return Chapter(
            id = UUID.randomUUID().toString(),
            title = "$number ${section.title}",
            nativeTitle = "$number ${response.nativeTitle?.takeIf { it.isNotBlank() } ?: section.title}",
            cards = cards.map(::toCard),
            part = section.part?.let { partRefs[it] },
        ).also { chapters += it }
    }

    /**
     * Creates the deck's details without chapters; every call describes the same deck (same id).
     *
     * @param title the book's title.
     * @param nativeTitle the translated title, or null to reuse [title].
     * @param frontLang the book's language code.
     * @param backLang the learner's language code.
     * @return the deck with no chapters.
     */
    fun header(title: String, nativeTitle: String?, frontLang: String, backLang: String): Deck =
        Deck(deckId, title, nativeTitle ?: title, emptyList(), frontLang, backLang)

    /**
     * Creates the deck.
     *
     * @param title the book's title.
     * @param nativeTitle the translated title, or null to reuse [title].
     * @param frontLang the book's language code.
     * @param backLang the learner's language code.
     * @return the deck with all chapters added so far.
     */
    fun build(title: String, nativeTitle: String?, frontLang: String, backLang: String): Deck =
        header(title, nativeTitle, frontLang, backLang).copy(chapters = chapters.toList())

    /**
     * Converts a generated card to a deck card, trimming the text and dropping empty transcriptions.
     *
     * @param card the generated card.
     * @return the card with a fresh id.
     */
    private fun toCard(card: GeneratedCard): Card = Card(
        id = UUID.randomUUID().toString(),
        front = Side(card.word.trim(), card.transcription?.trim()?.takeIf { it.isNotEmpty() }, card.example?.trim()),
        back = Side(card.translation.trim(), null, card.translatedExample?.trim()),
    )
}
