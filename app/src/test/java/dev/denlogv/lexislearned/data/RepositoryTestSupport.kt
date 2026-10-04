package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Card
import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.domain.PartRef
import dev.denlogv.lexislearned.domain.Side
import dev.denlogv.lexislearned.sampleDeck

/** One day in milliseconds. */
const val DAY = 24 * 60 * 60_000L

/**
 * The sample deck with every chapter but the first put into one part.
 *
 * @return the deck.
 */
fun partedDeck(): Deck {
    val part = PartRef("p1", "Part One", "Часть первая")
    return sampleDeck().let { d -> d.copy(chapters = d.chapters.mapIndexed { i, c -> if (i > 0) c.copy(part = part) else c }) }
}

/**
 * A card whose texts are derived from its id.
 *
 * @param id the card's id.
 * @return the card.
 */
fun simpleCard(id: String) = Card(id, Side("w-$id"), Side("t-$id"))
