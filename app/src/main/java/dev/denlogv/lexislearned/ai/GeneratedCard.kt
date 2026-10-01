package dev.denlogv.lexislearned.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One card as the model returns it. The JSON field names are fixed by the prompt (see [Prompts]).
 *
 * @property word the word or phrase in the book's language ("a").
 * @property translation its translation into the learner's language ("b").
 * @property transcription pronunciation of the word ("a_transcription"), if the model knew it.
 * @property example an original example sentence ("a_example").
 * @property translatedExample the sentence translated ("b_example").
 */
@Serializable
internal data class GeneratedCard(
    @SerialName("a") val word: String = "",
    @SerialName("b") val translation: String = "",
    @SerialName("a_transcription") val transcription: String? = null,
    @SerialName("a_example") val example: String? = null,
    @SerialName("b_example") val translatedExample: String? = null,
)

/**
 * The model's reply for one chapter.
 *
 * @property nativeTitle the chapter title translated into the learner's language ("chapter_title_b").
 * @property cards the cards for the chapter.
 */
@Serializable
internal data class ChapterResponse(
    @SerialName("chapter_title_b") val nativeTitle: String? = null,
    val cards: List<GeneratedCard> = emptyList(),
)

/**
 * The model's reply to the request for translated titles.
 *
 * @property title the book title translated into the learner's language ("title_b").
 * @property parts the part titles translated, keyed by the original title.
 */
@Serializable
internal data class TitleResponse(@SerialName("title_b") val title: String? = null, val parts: Map<String, String> = emptyMap())
