@file:Suppress("MaxLineLength") // The prompt texts are prose and read better as whole lines.

package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.CefrLevel

/**
 * The prompt for card generation, in three layers:
 *  1. a fixed header that defines the JSON reply (never editable, because the app parses it),
 *  2. the card rules, with a built-in default per CEFR level that the user may override,
 *  3. optional extra instructions from the user.
 *
 * Rules and extra instructions may contain the placeholders listed in [PLACEHOLDERS].
 */
object Prompts {
    /** Placeholders that are replaced in the card rules and the extra instructions. */
    val PLACEHOLDERS = listOf("{source_language}", "{target_language}", "{level}")

    /**
     * The built-in card rules for a level, with the placeholders still in place.
     *
     * @param level the learner's level.
     * @return the rules text.
     */
    fun defaultRules(level: CefrLevel): String {
        val p = LevelProfiles.of(level)
        return """
LEARNER
The learner's level is {level} (${level.title}). Make cards for vocabulary above that level: ${p.band}.

WHICH CARDS
Include:
${bullets(p.include)}
Exclude:
${bullets(p.exclude)}
$COMMON_EXCLUSIONS

$CARD_WRITING
- "a_example": ONE original sentence of ${p.exampleLength} words that uses the word in the same meaning (${p.exampleStyle}). $EXAMPLE_RULES
- Never copy passages of the book into cards.
        """.trimIndent()
    }

    /**
     * Assembles the system prompt: the fixed header, the rules and any extra instructions, with placeholders filled in.
     *
     * @param sourceLanguage the name of the book's language, for example "French".
     * @param targetLanguage the name of the learner's language, for example "Russian".
     * @param level the learner's level.
     * @param customRules the user's own rules for this level, or null or blank for the built-in ones.
     * @param extra extra instructions from the user; blank adds nothing.
     * @return the complete system prompt.
     */
    fun build(sourceLanguage: String, targetLanguage: String, level: CefrLevel, customRules: String? = null, extra: String = ""): String {
        val fill = { text: String -> fillPlaceholders(text, sourceLanguage, targetLanguage, level) }
        val rules = fill(customRules?.takeIf { it.isNotBlank() } ?: defaultRules(level))
        return header(sourceLanguage, targetLanguage) + "\n\n" + rules + extraBlock(extra, fill)
    }

    /**
     * Replaces the placeholders in a text.
     *
     * @param text the text, possibly containing the placeholders in [PLACEHOLDERS].
     * @param sourceLanguage replaces `{source_language}`.
     * @param targetLanguage replaces `{target_language}`.
     * @param level replaces `{level}` with its short label.
     * @return the text with the placeholders filled in.
     */
    private fun fillPlaceholders(text: String, sourceLanguage: String, targetLanguage: String, level: CefrLevel): String =
        text.replace("{source_language}", sourceLanguage).replace("{target_language}", targetLanguage).replace("{level}", level.label)

    /**
     * The fixed part of the prompt that defines the reply format.
     *
     * @param sourceLanguage the name of the book's language.
     * @param targetLanguage the name of the learner's language.
     * @return the header text.
     */
    private fun header(sourceLanguage: String, targetLanguage: String): String = """
You create pre-reading vocabulary flashcards from a chapter of a book. The reader should learn these words BEFORE reading.
The book is in $sourceLanguage; the reader's native language is $targetLanguage.
Reply with JSON only, exactly in this shape (field names are fixed):
{"chapter_title_b": "<chapter title rendered in $targetLanguage>",
 "cards": [{"a": "...", "b": "...", "a_transcription": "...", "a_example": "...", "b_example": "..."}]}
    """.trimIndent()

    /**
     * The block with the user's extra instructions.
     *
     * @param extra the instructions; blank yields an empty block.
     * @param fill replaces the placeholders in the text.
     * @return the block, or an empty string if there are no extra instructions.
     */
    private fun extraBlock(extra: String, fill: (String) -> String): String = extra.trim().takeIf { it.isNotEmpty() }
        ?.let { "\n\nADDITIONAL INSTRUCTIONS FROM THE USER (they never change the JSON format):\n${fill(it)}" }
        .orEmpty()

    /**
     * Formats a list as bullet lines.
     *
     * @param items the items.
     * @return one "- item" line per item.
     */
    private fun bullets(items: List<String>): String = items.joinToString("\n") { "- $it" }

    private const val COMMON_EXCLUSIONS = """- Proper nouns (characters, places), unless the name is also a common word whose meaning the reader needs.
- Trivially guessable cognates for a {target_language} speaker.
- Duplicates: one card per lemma, never separate cards for inflected forms. Typos and nonsense.
If there are more candidates than requested, prefer (1) words that block comprehension, (2) words that recur in the text, (3) idioms and fixed expressions, (4) rarer one-off words."""

    private const val CARD_WRITING = """HOW TO WRITE A CARD
- "a": dictionary form in {source_language}: nouns in the singular, verbs in the infinitive (English verbs with "to"), adjectives in the base form; expressions in a reusable form with "sb"/"sth" placeholders.
- "b": the meaning as used in the book, in {target_language}: short and clean, one or two equivalents separated by a comma. Idioms get the real idiomatic equivalent, not a word-for-word translation. No explanations or brackets.
- "a_transcription": IPA without slashes, with stress marks, matching the book's spelling variety. Omit the field if unsure."""

    private const val EXAMPLE_RULES =
        "It must NEVER come from or echo the book: no spoilers, characters or plot; " +
            "use a neutral everyday, work, travel or nature situation. " +
            "\"b_example\": its natural {target_language} translation."
}
