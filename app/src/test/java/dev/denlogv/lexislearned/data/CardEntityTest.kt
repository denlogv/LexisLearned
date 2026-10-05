package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Direction
import org.junit.Assert.assertEquals
import org.junit.Test

class CardEntityTest {
    private fun card(front: String?, back: String?) = CardEntity(
        deckId = 1,
        chapterId = 1,
        sourceId = "a",
        frontText = "word",
        frontTranscription = null,
        frontExample = front,
        frontGender = null,
        backText = "слово",
        backExample = back,
        backGender = null,
    )

    @Test
    fun revealedExamplesStartWithTheSentenceOfTheQuestionSide() {
        val card = card("A word.", "Слово.")
        assertEquals(listOf("A word.", "Слово."), card.revealedExamples(Direction.FORWARD))
        assertEquals(listOf("Слово.", "A word."), card.revealedExamples(Direction.REVERSE))
    }

    @Test
    fun revealedExamplesSkipMissingSentences() {
        assertEquals(listOf("Слово."), card(null, "Слово.").revealedExamples(Direction.FORWARD))
        assertEquals(emptyList<String>(), card(null, null).revealedExamples(Direction.REVERSE))
    }
}
