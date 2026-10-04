package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.epub.EpubBook
import dev.denlogv.lexislearned.epub.EpubChapter
import dev.denlogv.lexislearned.lorem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Generation neither asks the model for a number of cards nor drops cards because a chapter has many new words. */
class UnlimitedCardsTest {
    private val manyWords = Array(200) { "word$it" }

    private fun reply(vararg words: String) = """{"chapter_title_b":"Глава","cards":[${
        words.joinToString(",") { """{"a":"$it","b":"t-$it"}""" }
    }]}"""

    @Test
    fun fetcherAsksForNoCardCountAndKeepsEveryCardTheModelReturns() = runBlocking {
        val llm = ScriptedLlm { _, _ -> reply(*manyWords) }
        val response = ChapterCardFetcher(llm, "SYSTEM").fetch("Book", "Chapter", lorem)
        assertEquals(200, response.cards.size)
        assertTrue(llm.prompts.none { it.second.contains("Select about", ignoreCase = true) })
    }

    @Test
    fun aSectionIsNotCappedAtAnyNumberOfCards() = runBlocking {
        val llm = ScriptedLlm { _, user -> if (user.startsWith("Give")) "{}" else reply(*manyWords) }
        val book = EpubBook("Lorem", "en", listOf(EpubChapter(1, "One", lorem)))
        val result = CardGenerator(llm, "ru").generate(book)
        assertEquals(200, result.deck.chapters.single().cards.size)
    }
}
