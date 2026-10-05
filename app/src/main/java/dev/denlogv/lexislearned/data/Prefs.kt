package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode

/**
 * The AI providers a deck can be generated with.
 *
 * @property label the name shown to the user.
 * @property chip the short name on the provider selector.
 * @property defaultModel the model used until the user picks another.
 */
enum class Provider(val label: String, val chip: String, val defaultModel: String) {
    /** A server that speaks OpenAI's Chat Completions protocol, such as LM Studio or Ollama, at an address the user enters. */
    OPENAI_COMPATIBLE("OpenAI-compatible server", "OAI-compatible", ""),

    /** OpenAI. */
    OPENAI("OpenAI", "OpenAI", "gpt-4o-mini"),

    /** Anthropic (Claude). */
    ANTHROPIC("Anthropic (Claude)", "Claude", "claude-haiku-4-5-20251001"),
}

/**
 * The user's settings.
 *
 * @property direction which side of a card is the question.
 * @property modes the selected study modes; never empty.
 * @property sessionSize how many words one session contains.
 * @property newPerSession how many never-studied words a session may add.
 * @property rounds how many times each word goes through all selected modes within one session.
 * @property pairSize the most word pairs on one Pair board.
 * @property sessionsToComplete how many successful sessions make a word completed.
 * @property spaceSessions whether a word's sessions are spaced over days instead of allowing the next one right away.
 * @property provider the AI provider used to generate decks.
 * @property model the model id for the current provider; empty until chosen for [Provider.OPENAI_COMPATIBLE].
 * @property baseUrl the address of the server for [Provider.OPENAI_COMPATIBLE], as the user typed it.
 * @property targetLang the language code cards are translated into.
 * @property level the learner's CEFR level.
 * @property extraInstructions extra instructions appended to the generation prompt.
 * @property customRules the user's own card rules per level; a level without an entry uses the built-in rules.
 * @property hasApiKey whether an API key is saved for the current provider.
 */
data class Prefs(
    val direction: Direction = Direction.FORWARD,
    val modes: Set<StudyMode> = StudyMode.entries.toSet(),
    val sessionSize: Int = 10,
    val newPerSession: Int = 5,
    val rounds: Int = 1,
    val pairSize: Int = SessionPlanner.DEFAULT_PAIR_SIZE,
    val sessionsToComplete: Int = 1,
    val spaceSessions: Boolean = true,
    val provider: Provider = Provider.ANTHROPIC,
    val model: String = Provider.ANTHROPIC.defaultModel,
    val baseUrl: String = "",
    val targetLang: String = "ru",
    val level: CefrLevel = CefrLevel.B1,
    val extraInstructions: String = "",
    val customRules: Map<CefrLevel, String> = emptyMap(),
    val hasApiKey: Boolean = false,
) {
    /** The usable base URL of the custom server, or null if none is entered or it is invalid. */
    val endpoint: String? get() = normalizeBaseUrl(baseUrl)

    /** Whether the current provider has everything a request needs: a key, and a valid address for a custom server. */
    val ready: Boolean get() = hasApiKey && (provider != Provider.OPENAI_COMPATIBLE || (endpoint != null && model.isNotBlank()))
}

/** How many word pairs one Pair board may hold. */
val PAIR_SIZE_RANGE: IntRange = 3..12
