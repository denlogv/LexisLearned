package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.StudyMode

/**
 * The AI providers a deck can be generated with.
 *
 * @property label the name shown to the user.
 * @property defaultModel the model used until the user picks another.
 */
enum class Provider(val label: String, val defaultModel: String) {
    /** Anthropic (Claude). */
    ANTHROPIC("Anthropic (Claude)", "claude-haiku-4-5-20251001"),

    /** OpenAI. */
    OPENAI("OpenAI", "gpt-4o-mini"),
}

/**
 * The user's settings.
 *
 * @property direction which side of a card is the question.
 * @property modes the selected study modes; never empty.
 * @property sessionSize how many words one session contains.
 * @property newPerSession how many never-studied words a session may add.
 * @property rounds how many times each word goes through all selected modes within one session.
 * @property sessionsToComplete how many successful sessions make a word completed.
 * @property spaceSessions whether a word's sessions are spaced over days instead of allowing the next one right away.
 * @property provider the AI provider used to generate decks.
 * @property model the model id for the current provider.
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
    val sessionsToComplete: Int = 1,
    val spaceSessions: Boolean = true,
    val provider: Provider = Provider.ANTHROPIC,
    val model: String = Provider.ANTHROPIC.defaultModel,
    val targetLang: String = "ru",
    val level: CefrLevel = CefrLevel.B1,
    val extraInstructions: String = "",
    val customRules: Map<CefrLevel, String> = emptyMap(),
    val hasApiKey: Boolean = false,
)
