package dev.denlogv.lexislearned.ui.study

/** What a key does in a Select step. */
sealed interface SelectShortcut {
    /**
     * Picks an answer.
     *
     * @property option the answer.
     */
    data class Pick(val option: String) : SelectShortcut

    /** Moves on to the next step. */
    data object Next : SelectShortcut
}

/**
 * Works out what a key does in a Select step: a number key picks the answer at that position (until one is picked), then Enter
 * or Space moves on.
 *
 * @param key the key.
 * @param options the answers in the order they are shown.
 * @param picked the answer that was picked, or null if none yet.
 * @return what the key does, or null if it does nothing.
 */
fun selectShortcutFor(key: StepKey, options: List<String>, picked: String?): SelectShortcut? = when {
    key is StepKey.Number && picked == null -> options.getOrNull(key.value - 1)?.let { SelectShortcut.Pick(it) }
    (key == StepKey.Confirm || key == StepKey.Space) && picked != null -> SelectShortcut.Next
    else -> null
}

/**
 * Carries out a shortcut.
 *
 * @receiver the shortcut.
 * @param pick picks an answer.
 * @param next moves on to the next step.
 */
fun SelectShortcut.perform(pick: (String) -> Unit, next: () -> Unit) = when (this) {
    is SelectShortcut.Pick -> pick(option)
    SelectShortcut.Next -> next()
}
