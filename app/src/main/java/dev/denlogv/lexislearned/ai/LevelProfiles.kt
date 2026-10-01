@file:Suppress("MaxLineLength") // The prompt texts are prose and read better as whole lines.

package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.domain.CefrLevel

/**
 * What the built-in card rules say for one CEFR level.
 *
 * @property band describes the vocabulary to make cards for, for example "B2 and above".
 * @property include kinds of words and expressions to include.
 * @property exclude kinds of words to leave out, in addition to the rules every level shares.
 * @property exampleLength the number of words of an example sentence, for example "8-16".
 * @property exampleStyle how simple or rich the example sentences should be.
 */
internal class LevelProfile(
    val band: String,
    val include: List<String>,
    val exclude: List<String>,
    val exampleLength: String,
    val exampleStyle: String,
)

/** The per-level settings behind the built-in card rules (see [Prompts.defaultRules]). */
internal object LevelProfiles {
    /**
     * The profile of a level.
     *
     * @param level the learner's level.
     * @return what to include and exclude and how to write examples at that level.
     */
    fun of(level: CefrLevel): LevelProfile = profiles.getValue(level)

    private val profiles = mapOf(
        CefrLevel.A1 to LevelProfile(
            band = "A2 and a little beyond: the most useful everyday vocabulary a beginner will meet again and again",
            include = listOf(
                "Concrete, high-frequency nouns, verbs and adjectives for everyday life: home, food, shopping, transport, work, weather, feelings.",
                "Simple useful expressions and question forms (\"how much is...\", \"to look for\").",
                "Words that appear several times in the text, even if they are only slightly above the learner's level.",
            ),
            exclude = listOf(
                "A1 basics: numbers, colours, days, family members, greetings, articles, pronouns, \"to be\", \"to have\", the most common verbs.",
                "Rare, literary, technical, archaic or idiomatic vocabulary a beginner cannot use yet.",
            ),
            exampleLength = "5-9",
            exampleStyle = "present tense, very simple grammar, only common words apart from the target word",
        ),
        CefrLevel.A2 to LevelProfile(
            band = "B1: common but not basic vocabulary for everyday topics and simple stories",
            include = listOf(
                "Everyday nouns, verbs, adjectives and adverbs that go beyond survival vocabulary: opinions, plans, travel, health, work, hobbies.",
                "Simple phrasal verbs and fixed phrases with a clear meaning.",
                "Words that recur in the text or block understanding of a scene.",
            ),
            exclude = listOf(
                "A1-A2 core vocabulary and its regular inflected forms.",
                "Rare, literary, formal or highly specialised words.",
            ),
            exampleLength = "6-11",
            exampleStyle = "simple past and present, short sentences, no other difficult words",
        ),
        CefrLevel.B1 to LevelProfile(
            band = "B2 and above: uncommon, abstract, formal or literary vocabulary",
            include = listOf(
                "Single words a B1 learner would probably not know: uncommon nouns, verbs, adjectives, adverbs; abstract, literary, formal or journalistic vocabulary; specialised terms that matter for the book's topic or setting.",
                "Expressions: idioms, fixed phrases, collocations, phrasal verbs (also separable or reflexive ones) with a non-obvious meaning, proverbs, typical B2+ discourse phrases.",
                "Central recurring key terms, even if only borderline B2.",
                "Words used in a figurative or unusual sense whose meaning here differs from the common B1 meaning.",
            ),
            exclude = listOf(
                "A1-B1 core vocabulary and its regular inflected forms.",
                "Transparent compounds made of B1 words, unless the exact meaning is non-obvious.",
            ),
            exampleLength = "8-16",
            exampleStyle = "natural, clear, at a level a B2 learner can read, no other rare words",
        ),
        CefrLevel.B2 to LevelProfile(
            band = "C1 and above: less common, nuanced, formal, literary and idiomatic vocabulary",
            include = listOf(
                "Low-frequency abstract nouns, precise verbs, nuanced adjectives and adverbs, formal and literary register.",
                "Idioms, collocations, phrasal verbs and discourse markers typical of advanced writing.",
                "Specialised terms that matter for the book (law, science, medicine, sailing, politics...).",
                "Words used in a figurative, ironic or unusual sense.",
            ),
            exclude = listOf(
                "A1-B2 vocabulary and its regular inflected forms.",
                "Words that are transparent from a Romance/Germanic root the learner already knows, unless the exact meaning is non-obvious.",
            ),
            exampleLength = "10-18",
            exampleStyle = "natural and idiomatic, at a level a C1 learner can read",
        ),
        CefrLevel.C1 to LevelProfile(
            band = "C2 and beyond: rare, literary, technical and highly idiomatic vocabulary",
            include = listOf(
                "Rare and literary words, archaisms that still occur in books, precise technical terms, nuanced near-synonyms.",
                "Idioms, proverbs, sayings, allusions and register-marked expressions (slang, dialect, jargon) that an advanced learner may still miss.",
                "Words whose meaning here differs subtly from their common meaning.",
            ),
            exclude = listOf(
                "Anything up to C1, including formal and academic vocabulary the learner already commands.",
                "Proper nouns, unless they are also common words.",
            ),
            exampleLength = "12-20",
            exampleStyle = "fluent, idiomatic, with natural collocations",
        ),
        CefrLevel.C2 to LevelProfile(
            band = "the rarest layer: obscure, archaic, dialectal, highly specialised or very literary words and expressions",
            include = listOf(
                "Words even well-read native speakers may have to look up: archaic, dialectal, technical or highly literary vocabulary.",
                "Rare idioms, proverbs, allusions and period- or region-specific expressions.",
                "Terms that are essential for understanding this particular book, even if highly specialised.",
            ),
            exclude = listOf(
                "Everything a proficient (C2) reader knows, including ordinary academic and literary vocabulary.",
                "Invented words and names from the book's fictional world, unless they are built from real rare words.",
            ),
            exampleLength = "12-22",
            exampleStyle = "polished and idiomatic, the way a good writer would use the word",
        ),
    )
}
