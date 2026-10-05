package dev.denlogv.lexislearned.ui.settings

import dev.denlogv.lexislearned.ai.ModelInfo
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.ui.plural

/**
 * Explains how long a full study session is.
 *
 * @param prefs the session settings.
 * @return a sentence such as "A full session is about 30 steps (10 words × 3 modes × 1 round)."
 */
fun sessionSummary(prefs: Prefs): String {
    val steps = plural(prefs.sessionSize * prefs.rounds * prefs.modes.size, "step")
    val words = plural(prefs.sessionSize, "word")
    val modes = plural(prefs.modes.size, "mode")
    val rounds = plural(prefs.rounds, "round")
    return "A full session is about $steps ($words × $modes × $rounds). Choose the modes on a book, part or chapter screen."
}

/**
 * The text on the button that opens the model menu.
 *
 * @param ui the model list state.
 * @param selected the id of the model in use.
 * @return the model's name, or a note about why there is none to show.
 */
fun modelButtonLabel(ui: ModelsUi, selected: String): String = when {
    ui.models.firstOrNull { it.id == selected } != null -> ui.models.first { it.id == selected }.label
    ui.models.isNotEmpty() -> selected
    ui.loading -> "Loading models…"
    else -> "No models loaded"
}

/**
 * The text of one entry in the model menu.
 *
 * @param model the model.
 * @return its name, followed by the id when the two differ.
 */
fun modelMenuLabel(model: ModelInfo): String = if (model.label == model.id) model.id else "${model.label} (${model.id})"

/**
 * The label of the card rules field.
 *
 * @param levelLabel the level, for example "B1".
 * @param customised whether the rules differ from the built-in ones.
 * @return the label text.
 */
fun rulesFieldLabel(levelLabel: String, customised: Boolean): String =
    "Card rules for $levelLabel" + if (customised) " (customised)" else " (default)"

/**
 * The one-line summary of the study settings on the settings overview.
 *
 * @param prefs the settings.
 * @return for example "10 cards per session · 5 new · 1 round · pairs of 6".
 */
fun studySummary(prefs: Prefs): String =
    "${plural(prefs.sessionSize, "card")} per session · ${prefs.newPerSession} new · ${plural(prefs.rounds, "round")} · " +
        "pairs of ${prefs.pairSize}"

/**
 * The one-line summary of the deck generation settings on the settings overview.
 *
 * @param prefs the settings.
 * @return the provider and the model in use, and whether a key is saved, for example "Claude · claude-haiku-4-5 · key saved".
 */
fun generationSummary(prefs: Prefs): String = listOfNotNull(
    prefs.provider.chip,
    prefs.model.takeIf { it.isNotBlank() },
    if (prefs.hasApiKey) "key saved" else "no key yet",
).joinToString(" · ")

/**
 * The one-line summary of the level and card prompt settings on the settings overview.
 *
 * @param prefs the settings.
 * @return the level, whether its rules were edited and whether there are extra instructions, for example
 * "B1 · customised rules · extra instructions".
 */
fun promptSummary(prefs: Prefs): String = listOfNotNull(
    prefs.level.label,
    if (prefs.customRules.containsKey(prefs.level)) "customised rules" else "default rules",
    "extra instructions".takeIf { prefs.extraInstructions.isNotBlank() },
).joinToString(" · ")
