package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.domain.Card
import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.domain.Side

/** Generated placeholder deck (no real book content). */
fun sampleDeck() = Deck(
    id = "deck-1",
    title = "Lorem",
    nativeTitle = "Лорем",
    chapters = (1..3).map { c ->
        Chapter(
            id = "ch-$c",
            title = "Chapter $c",
            nativeTitle = "Глава $c",
            cards = (1..4).map { n ->
                Card(
                    id = "card-$c-$n",
                    front = Side("lorem$c$n", "ˈlɔːrəm", "Lorem ipsum dolor sit amet $c$n."),
                    back = Side("ипсум$c$n", null, "Пример $c$n."),
                )
            },
        )
    },
)
