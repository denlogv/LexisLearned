package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.domain.PartRef
import dev.denlogv.lexislearned.epub.EpubBook
import dev.denlogv.lexislearned.epub.EpubChapter
import dev.denlogv.lexislearned.epub.EpubReader
import dev.denlogv.lexislearned.lorem
import dev.denlogv.lexislearned.loremEpub
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** An LLM that answers from a function and records what it was asked. */
class ScriptedLlm(private val answer: (system: String, user: String) -> String) : LlmClient {
    val prompts = mutableListOf<Pair<String, String>>()

    override suspend fun complete(system: String, user: String): String {
        prompts += system to user
        return answer(system, user)
    }
}

class GenerationTest {
    private val book = EpubBook(
        "Lorem",
        "en",
        listOf(
            EpubChapter(1, "One", lorem, "Part I"),
            EpubChapter(2, "Two", lorem, "Part I"),
            EpubChapter(3, "Three", lorem, null),
        ),
    )

    private fun reply(vararg words: String) = """{"chapter_title_b":"Глава","cards":[${
        words.joinToString(",") { """{"a":"$it","b":"t-$it","a_transcription":" tr ","a_example":"ex","b_example":"пр"}""" }
    }]}"""

    @Test
    fun splitterKeepsShortTextsAndCutsLongOnesAtLines() {
        assertEquals(listOf("abc"), TextSplitter.split("abc", 10))
        val parts = TextSplitter.split("aaaa\nbbbb\ncccc", 9)
        assertEquals(listOf("aaaa\nbbbb\n", "cccc\n"), parts)
    }

    @Test
    fun jsonIsExtractedFromChattyReplies() {
        val parsed = LlmJson.parse("Sure! {\"title_b\":\"X\",\"extra\":1} hope it helps", TitleResponse.serializer())
        assertEquals("X", parsed.title)
        assertThrows(LlmException::class.java) { LlmJson.parse("no json here", TitleResponse.serializer()) }
        assertThrows(SerializationException::class.java) { LlmJson.parse("{\"parts\": 5}", TitleResponse.serializer()) }
    }

    @Test
    fun languageNamesComeFromTheLocale() {
        assertEquals("German", Languages.nameOf("de"))
        assertEquals("Russian", Languages.nameOf("ru"))
        assertEquals("", Languages.nameOf(""))
    }

    @Test
    fun titleTranslatorFallsBackOnNonAuthErrors() = runBlocking {
        val ok = TitleTranslator(ScriptedLlm { _, _ -> """{"title_b":"Лорем","parts":{"Part I":"Часть I"}}""" }, "Russian")
        val titles = ok.translate("Lorem", listOf("Part I"))
        assertEquals("Лорем", titles.book)
        assertEquals("Часть I", titles.parts["Part I"])
        for (failure in listOf(LlmException("x", 500), SerializationException("bad"), IOException("net"))) {
            val broken = TitleTranslator(ScriptedLlm { _, _ -> throw failure }, "Russian")
            assertNull(broken.translate("Lorem", emptyList()).book)
        }
        val rejected = TitleTranslator(ScriptedLlm { _, _ -> throw LlmException("no", 401) }, "Russian")
        assertThrows(LlmException::class.java) { runBlocking { rejected.translate("Lorem", emptyList()) } }
        Unit
    }

    @Test
    fun assemblerDropsDuplicatesEmptyCardsAndEmptyChapters() {
        val part = PartRef("p", "Part I")
        val assembler = DeckAssembler(mapOf("Part I" to part))
        val cards =
            listOf(GeneratedCard("Alpha", "альфа"), GeneratedCard("alpha ", "дубль"), GeneratedCard("", "x"), GeneratedCard("b", ""))
        assembler.add(book.chapters[0], ChapterResponse("Один", cards))
        assembler.add(book.chapters[1], ChapterResponse(null, listOf(GeneratedCard("ALPHA", "again"))))
        assembler.add(book.chapters[2], ChapterResponse(" ", listOf(GeneratedCard("gamma", "гамма", " tr "))))
        val deck = assembler.build("Lorem", null, "en", "ru")
        assertEquals(2, deck.chapters.size)
        assertEquals("01 One", deck.chapters[0].title)
        assertEquals("01 Один", deck.chapters[0].nativeTitle)
        assertEquals(part, deck.chapters[0].part)
        assertEquals("02 Three", deck.chapters[1].nativeTitle)
        assertEquals("tr", deck.chapters[1].cards.single().front.transcription)
        assertEquals("Lorem", deck.nativeTitle)
        assertEquals(2, assembler.cardCount)
    }

    @Test
    fun fetcherSplitsLongChaptersAndMergesTheAnswers() = runBlocking {
        val llm = ScriptedLlm { _, user -> reply("w${user.length}") }
        val long = (1..3000).joinToString("\n") { "line $it lorem ipsum dolor sit amet consectetur" }
        val response = ChapterCardFetcher(llm, "SYSTEM").fetch("Book", "Chapter", long, 12)
        assertTrue(llm.prompts.size > 1)
        assertEquals(llm.prompts.size, response.cards.size)
        assertEquals("Глава", response.nativeTitle)
        assertTrue(llm.prompts.all { it.first == "SYSTEM" && it.second.contains("Chapter") })
    }

    @Test
    fun generatorBuildsADeckReportsProgressAndHonoursSelection() = runBlocking {
        val llm = ScriptedLlm { system, user ->
            when {
                system.startsWith("Reply with JSON only") -> """{"title_b":"Лорем","parts":{"Part I":"Часть I"}}"""
                user.contains("Chapter: \"One\"") -> reply("a", "b")
                else -> reply("c")
            }
        }
        val progress = mutableListOf<String>()
        val options = GenerationOptions(selected = setOf(1, 3), sourceLang = "en", onProgress = { _, _, _, m -> progress += m })
        val result = CardGenerator(llm, "ru", CefrLevel.B2, extraInstructions = "legal").generate(book, options)
        assertEquals(listOf("One", "Three", "Done"), progress)
        assertEquals(3, result.deck.cardCount)
        assertEquals("Лорем", result.deck.nativeTitle)
        assertEquals("Часть I", result.deck.chapters[0].part?.nativeTitle)
        assertNull(result.deck.chapters[1].part)
        assertTrue(result.failedChapters.isEmpty())
        assertTrue(llm.prompts.any { it.first.contains("legal") })
    }

    @Test
    fun generatorRecordsFailedSectionsAndStopsWhenCancelled() = runBlocking {
        val llm = ScriptedLlm { system, user ->
            when {
                system.startsWith("Reply with JSON only") -> "{}"
                user.contains("Chapter: \"One\"") -> "not json"
                user.contains("Chapter: \"Two\"") -> throw LlmException("overloaded", 500)
                else -> reply("z")
            }
        }
        val failed = CardGenerator(llm, "ru").generate(book).failedChapters
        assertEquals(2, failed.size)
        assertTrue(failed[1].startsWith("Two (") && failed[1].contains("overloaded"))
        var calls = 0
        val stopped = CardGenerator(ScriptedLlm { _, _ -> reply("q") }, "ru")
            .generate(book, GenerationOptions(isCancelled = { calls++ > 0 }))
        assertEquals(1, stopped.deck.chapters.size)
    }

    @Test
    fun progressCountsOnlySectionsThatReachedTheDeck() = runBlocking {
        val llm = ScriptedLlm { system, user ->
            when {
                system.startsWith("Reply with JSON only") -> "{}"
                user.contains("Chapter: \"Two\"") -> "not json" // fails
                else -> reply("a") // One adds it, Three has nothing new
            }
        }
        val counts = mutableListOf<Int>()
        CardGenerator(llm, "ru").generate(book, GenerationOptions(onProgress = { done, _, _, _ -> counts += done }))
        assertEquals(listOf(0, 1, 1, 1), counts) // before One, before Two, before Three, at the end
    }

    @Test
    fun generatorStopsOnRejectedKey() {
        val llm = ScriptedLlm { system, _ -> if (system.startsWith("Reply")) "{}" else throw LlmException("bad key", 401) }
        assertThrows(LlmException::class.java) { runBlocking { CardGenerator(llm, "ru").generate(book) } }
    }

    @Test
    fun generatorBuildsADeckFromAnEpubAndDropsDuplicates() = runBlocking {
        val prompts = ArrayList<String>()
        val llm = object : LlmClient {
            override suspend fun complete(system: String, user: String): String {
                prompts += user
                return if (user.startsWith("Give the")) {
                    """{"title_b": "Лорем"}"""
                } else {
                    """Sure! {"chapter_title_b": "Глава", "cards": [
                    {"a": "gaunt", "b": "измождённый", "a_transcription": "ɡɔːnt", "a_example": "He looked gaunt.", "b_example": "Он выглядел измождённым."},
                    {"a": "Gaunt", "b": "дубликат"}, {"a": "", "b": "пусто"}]}"""
                }
            }
        }
        val book = EpubReader.read(loremEpub().inputStream())
        val result = CardGenerator(llm, "ru").generate(book)
        val deck = result.deck
        assertEquals("en", deck.frontLang)
        assertEquals("Лорем", deck.nativeTitle)
        // second chapter only had duplicates, so it is dropped
        assertEquals(1, deck.chapters.size)
        assertEquals("01 Chapter One", deck.chapters[0].title)
        assertEquals("01 Глава", deck.chapters[0].nativeTitle)
        assertEquals("gaunt", deck.chapters[0].cards.single().front.text)
        assertTrue(prompts.any { it.contains("CHAPTER TEXT") })
    }
}
