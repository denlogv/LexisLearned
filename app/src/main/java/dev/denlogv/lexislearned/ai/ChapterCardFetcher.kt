package dev.denlogv.lexislearned.ai

/**
 * Asks the model for the vocabulary cards of one chapter, splitting very long chapters over several requests.
 *
 * @param llm the model.
 * @param systemPrompt the system prompt with the card rules (see [Prompts.build]).
 */
internal class ChapterCardFetcher(private val llm: LlmClient, private val systemPrompt: String) {
    /**
     * Fetches the cards of a chapter.
     *
     * @param bookTitle the book's title, given to the model as context.
     * @param chapterTitle the chapter's title.
     * @param text the chapter's text.
     * @return the cards of all parts and the chapter's translated title, if the model gave one.
     * @throws LlmException if a request fails or the reply is not JSON.
     * @throws kotlinx.serialization.SerializationException if a reply does not have the expected shape.
     */
    suspend fun fetch(bookTitle: String, chapterTitle: String, text: String): ChapterResponse {
        val parts = TextSplitter.split(text, MAX_PART_CHARS)
        val responses = parts.mapIndexed { i, part ->
            val reply = llm.complete(systemPrompt, userPrompt(bookTitle, chapterTitle, i + 1, parts.size, part))
            LlmJson.parse(reply, ChapterResponse.serializer())
        }
        return ChapterResponse(responses.firstNotNullOfOrNull { it.nativeTitle }, responses.flatMap { it.cards })
    }

    /**
     * The request text for one part of a chapter.
     *
     * @param book the book's title.
     * @param chapter the chapter's title.
     * @param number the 1-based number of this part.
     * @param count how many parts the chapter was split into.
     * @param text the part's text.
     * @return the user message.
     */
    private fun userPrompt(book: String, chapter: String, number: Int, count: Int, text: String): String =
        "Book: \"$book\"\nChapter: \"$chapter\" (part $number of $count)\n\n" +
            "--- CHAPTER TEXT ---\n$text"

    private companion object {
        /** Longest piece of chapter text sent in one request. */
        const val MAX_PART_CHARS = 24_000
    }
}
