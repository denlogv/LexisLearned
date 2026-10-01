package dev.denlogv.lexislearned.ai

import java.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Asks the model for translated titles of the book and its parts in one cheap request.
 *
 * @param llm the model.
 * @param targetName the name of the learner's language, for example "Russian".
 */
internal class TitleTranslator(private val llm: LlmClient, private val targetName: String) {
    /**
     * Translated titles.
     *
     * @property book the book title translated, or null if the request failed.
     * @property parts the part titles translated, keyed by the original title; may lack entries.
     */
    class Titles(val book: String?, val parts: Map<String, String>)

    /**
     * Translates the titles. A failure is not fatal: the titles are then simply left untranslated.
     *
     * @param bookTitle the book's title.
     * @param partTitles the titles of the parts that will be used.
     * @return the translations that could be obtained.
     */
    @Suppress("SwallowedException") // Translating titles is optional: any failure except a rejected key means "no translation".
    suspend fun translate(bookTitle: String, partTitles: List<String>): Titles = try {
        val reply = llm.complete(SYSTEM_PROMPT, userPrompt(bookTitle, partTitles))
        val parsed = LlmJson.parse(reply, TitleResponse.serializer())
        Titles(parsed.title, parsed.parts)
    } catch (e: LlmException) {
        if (e.code in AUTH_ERRORS) throw e
        Titles(null, emptyMap())
    } catch (e: SerializationException) {
        Titles(null, emptyMap())
    } catch (e: IOException) {
        Titles(null, emptyMap())
    }

    /**
     * The request text.
     *
     * @param bookTitle the book's title.
     * @param partTitles the part titles to translate.
     * @return the user message.
     */
    private fun userPrompt(bookTitle: String, partTitles: List<String>): String =
        "Give the $targetName title of the book \"$bookTitle\" if it has a well-known translation, " +
            "otherwise repeat the original title. Also translate these part titles into $targetName " +
            "(keep the original if it is a proper name): ${partTitles.joinToString(" | ")}"

    private companion object {
        const val SYSTEM_PROMPT = "Reply with JSON only: {\"title_b\": \"...\", \"parts\": {\"<original part title>\": \"<translation>\"}}"
        val AUTH_ERRORS = setOf(401, 403)
    }
}
