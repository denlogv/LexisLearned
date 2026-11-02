package dev.denlogv.lexislearned.ui.study

/** What a key does in a Type step, outside the answer field. */
enum class TypeShortcut {
    /** "I don't know": shows the answer and counts as a miss. */
    GIVE_UP,

    /** Moves on to the next step. */
    NEXT,
}

/**
 * Works out what a key does in a Type step when the focus is not in the answer field, which is after the answer was checked.
 * Pressing Enter in the field checks the answer; that is the field's own action and not decided here. Ctrl+D gives up until
 * the answer is checked, and then Enter or Space moves on.
 *
 * @receiver the state of the step at the moment of the key press.
 * @param key the key.
 * @return what the key does, or null if it does nothing.
 */
fun TypeState.shortcutFor(key: StepKey): TypeShortcut? = when {
    key == StepKey.GiveUp && !checked -> TypeShortcut.GIVE_UP
    (key == StepKey.Confirm || key == StepKey.Space) && checked -> TypeShortcut.NEXT
    else -> null
}

/**
 * Carries out a shortcut.
 *
 * @receiver the shortcut.
 * @param giveUp gives up the question.
 * @param next moves on to the next step.
 */
fun TypeShortcut.perform(giveUp: () -> Unit, next: () -> Unit) = when (this) {
    TypeShortcut.GIVE_UP -> giveUp()
    TypeShortcut.NEXT -> next()
}
