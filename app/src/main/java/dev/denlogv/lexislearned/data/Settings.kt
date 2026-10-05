package dev.denlogv.lexislearned.data

import android.content.SharedPreferences
import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.StudyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Reads and writes the user's settings; every change is saved at once and published through [prefs].
 *
 * @param sp where the settings are stored.
 * @param secrets where API keys are stored.
 */
@Suppress("TooManyFunctions") // One setter per preference.
class Settings(private val sp: SharedPreferences, private val secrets: Secrets = KeystoreSecrets(sp)) {
    private val _prefs = MutableStateFlow(load())

    /** The current settings. */
    val prefs: StateFlow<Prefs> = _prefs

    /**
     * Sets which side of a card is the question.
     *
     * @param d the direction.
     */
    fun setDirection(d: Direction) = edit { putString(DIRECTION, d.name) }

    /**
     * Selects or deselects a study mode; at least one mode always stays selected.
     *
     * @param m the mode to toggle.
     */
    fun toggleMode(m: StudyMode) {
        val current = _prefs.value.modes
        val next = if (m in current) current - m else current + m
        if (next.isNotEmpty()) edit { putString(MODES, StudyMode.entries.filter { it in next }.joinToString(",") { it.name }) }
    }

    /**
     * Sets how many words one session contains.
     *
     * @param n the number of words.
     */
    fun setSessionSize(n: Int) = edit { putInt(SESSION_SIZE, n) }

    /**
     * Sets how many never-studied words a session may add.
     *
     * @param n the number of words.
     */
    fun setNewPerSession(n: Int) = edit { putInt(NEW_PER_SESSION, n) }

    /**
     * Sets how many times each word goes through all modes in a session.
     *
     * @param n the number of rounds, limited to 1 to 5.
     */
    fun setRounds(n: Int) = edit { putInt(ROUNDS, n.coerceIn(MIN_ROUNDS, MAX_ROUNDS)) }

    /**
     * Sets how many word pairs a Pair board holds at most.
     *
     * @param n the number of pairs, limited to [PAIR_SIZE_RANGE].
     */
    fun setPairSize(n: Int) = edit { putInt(PAIR_SIZE, n.coerceIn(PAIR_SIZE_RANGE)) }

    /**
     * Sets how many successful sessions complete a word.
     *
     * @param n the number of sessions, limited to 1 to 10.
     */
    fun setSessionsToComplete(n: Int) = edit { putInt(SESSIONS_TO_COMPLETE, n.coerceIn(MIN_SESSIONS, MAX_SESSIONS)) }

    /**
     * Sets whether a word's sessions are spaced over days.
     *
     * @param on true to use spaced repetition.
     */
    fun setSpaceSessions(on: Boolean) = edit { putBoolean(SPACE_SESSIONS, on) }

    /**
     * Chooses the AI provider.
     *
     * @param p the provider.
     */
    fun setProvider(p: Provider) = edit { putString(PROVIDER, p.name) }

    /**
     * Chooses the model for the current provider.
     *
     * @param m the model id.
     */
    fun setModel(m: String) = edit { putString(MODEL_PREFIX + _prefs.value.provider.name, m.trim()) }

    /**
     * Sets the address of the server used by [Provider.OPENAI_COMPATIBLE].
     *
     * @param url the address as typed; it is kept as typed and checked when used, see [Prefs.endpoint].
     */
    fun setBaseUrl(url: String) = edit { putString(BASE_URL, url.trim()) }

    /**
     * Sets the learner's level.
     *
     * @param l the level.
     */
    fun setLevel(l: CefrLevel) = edit { putString(LEVEL, l.name) }

    /**
     * Sets the extra instructions appended to the generation prompt.
     *
     * @param t the instructions.
     */
    fun setExtraInstructions(t: String) = edit { putString(EXTRA_INSTRUCTIONS, t) }

    /**
     * Saves custom card rules for a level; blank text removes the override so the built-in rules apply again.
     *
     * @param l the level.
     * @param text the rules.
     */
    fun setCustomRules(l: CefrLevel, text: String) = edit {
        if (text.isBlank()) remove(RULES_PREFIX + l.name) else putString(RULES_PREFIX + l.name, text)
    }

    /**
     * Sets the language cards are translated into.
     *
     * @param code an ISO 639-1 code; stored trimmed and in lower case.
     */
    fun setTargetLang(code: String) = edit { putString(TARGET_LANG, code.trim().lowercase()) }

    /**
     * The saved API key of a provider.
     *
     * @param provider the provider; the current one by default.
     * @return the key, or null if none is saved.
     */
    fun apiKey(provider: Provider = _prefs.value.provider): String? = secrets.get(provider.name)

    /**
     * Saves the API key of a provider.
     *
     * @param provider the provider.
     * @param key the key; blank removes it.
     */
    fun setApiKey(provider: Provider, key: String) {
        secrets.put(provider.name, key.trim())
        _prefs.value = load()
    }

    /**
     * Applies a change to the stored settings and publishes the result.
     *
     * @param block the change to make on the preferences editor.
     */
    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        sp.edit().apply(block).apply()
        _prefs.value = load()
    }

    /**
     * Reads all settings.
     *
     * @return the stored settings, with defaults for anything missing or invalid.
     */
    private fun load(): Prefs = loadGeneration(loadStudy(Prefs()))

    /**
     * Reads the study-related settings.
     *
     * @param base the settings to start from.
     * @return [base] with the study settings filled in.
     */
    private fun loadStudy(base: Prefs): Prefs = base.copy(
        direction = enumOrDefault(DIRECTION, base.direction),
        modes = sp.getString(MODES, null)?.split(',')
            ?.mapNotNull { name -> StudyMode.entries.firstOrNull { it.name == name } }?.toSet()
            ?.takeIf { it.isNotEmpty() } ?: base.modes,
        sessionSize = sp.getInt(SESSION_SIZE, base.sessionSize),
        newPerSession = sp.getInt(NEW_PER_SESSION, base.newPerSession),
        rounds = sp.getInt(ROUNDS, base.rounds),
        pairSize = sp.getInt(PAIR_SIZE, base.pairSize).coerceIn(PAIR_SIZE_RANGE),
        sessionsToComplete = sp.getInt(SESSIONS_TO_COMPLETE, base.sessionsToComplete),
        spaceSessions = sp.getBoolean(SPACE_SESSIONS, base.spaceSessions),
    )

    /**
     * Reads the settings for deck generation.
     *
     * @param base the settings to start from.
     * @return [base] with the provider, model, language, level and prompt settings filled in.
     */
    private fun loadGeneration(base: Prefs): Prefs {
        val provider = enumOrDefault(PROVIDER, base.provider)
        return base.copy(
            provider = provider,
            model = sp.getString(MODEL_PREFIX + provider.name, null) ?: provider.defaultModel,
            baseUrl = sp.getString(BASE_URL, null) ?: base.baseUrl,
            targetLang = sp.getString(TARGET_LANG, null) ?: base.targetLang,
            level = enumOrDefault(LEVEL, base.level),
            extraInstructions = sp.getString(EXTRA_INSTRUCTIONS, null) ?: base.extraInstructions,
            customRules = CefrLevel.entries.mapNotNull { level ->
                sp.getString(RULES_PREFIX + level.name, null)?.takeIf { it.isNotBlank() }?.let { level to it }
            }.toMap(),
            hasApiKey = secrets.get(provider.name) != null,
        )
    }

    /**
     * Reads an enum setting.
     *
     * @param key the preference key.
     * @param default the value to use when nothing valid is stored.
     * @return the stored value, or [default].
     */
    private inline fun <reified E : Enum<E>> enumOrDefault(key: String, default: E): E =
        sp.getString(key, null)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private companion object {
        const val DIRECTION = "direction"
        const val MODES = "modes"
        const val SESSION_SIZE = "sessionSize"
        const val NEW_PER_SESSION = "newPerSession"
        const val ROUNDS = "rounds"
        const val PAIR_SIZE = "pairSize"
        const val SESSIONS_TO_COMPLETE = "sessionsToComplete"
        const val SPACE_SESSIONS = "spaceSessions"
        const val PROVIDER = "provider"
        const val MODEL_PREFIX = "model_"
        const val BASE_URL = "baseUrl"
        const val TARGET_LANG = "targetLang"
        const val LEVEL = "level"
        const val EXTRA_INSTRUCTIONS = "extraInstructions"
        const val RULES_PREFIX = "rules_"
        const val MIN_ROUNDS = 1
        const val MAX_ROUNDS = 5
        const val MIN_SESSIONS = 1
        const val MAX_SESSIONS = 10
    }
}
