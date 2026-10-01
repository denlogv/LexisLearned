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
 */
internal class DeckAssembler(private val partRefs: Map<String, PartRef>) {
    private val seen = HashSet<String>()
    private val chapters = ArrayList<Chapter>()

    /** Number of cards added so far. */
    val cardCount: Int get() = chapters.sumOf { it.cards.size }

    /**
     * Adds a chapter's cards. A chapter left without cards after filtering is not added.
     *
     * @param section the book section the cards were made for.
     * @param response the model's reply for the section.
     */
    fun add(section: EpubChapter, response: ChapterResponse) {
        val cards = response.cards.filter { it.word.isNotBlank() && it.translation.isNotBlank() && seen.add(it.word.trim().lowercase()) }
        if (cards.isEmpty()) return
        val number = "%02d".format(chapters.size + 1)
        chapters += Chapter(
            id = UUID.randomUUID().toString(),
            title = "$number ${section.title}",
            nativeTitle = "$number ${response.nativeTitle?.takeIf { it.isNotBlank() } ?: section.title}",
            cards = cards.map(::toCard),
            part = section.part?.let { partRefs[it] },
        )
    }

    /**
     * Creates the deck.
     *
     * @param title the book's title.
     * @param nativeTitle the translated title, or null to reuse [title].
     * @param frontLang the book's language code.
     * @param backLang the learner's language code.
     * @return the deck with all chapters added so far.
     */
    fun build(title: String, nativeTitle: String?, frontLang: String, backLang: String): Deck = Deck(
        id = UUID.randomUUID().toString(),
        title = title,
        nativeTitle = nativeTitle ?: title,
        chapters = chapters.toList(),
        frontLang = frontLang,
        backLang = backLang,
    )

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
