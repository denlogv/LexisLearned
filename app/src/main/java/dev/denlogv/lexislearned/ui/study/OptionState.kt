package dev.denlogv.lexislearned.ui.study

/** How an answer choice of a Select step looks. */
enum class OptionState {
    /** Nothing picked yet; the choice can be tapped. */
    OPEN,

    /** The right answer, shown in green once something was picked. */
    CORRECT,

    /** The learner's wrong pick, shown in red. */
    WRONG,

    /** Any other choice after a pick, left plain. */
    DIMMED,
}

/**
 * Works out how a choice should look.
 *
 * @param option the choice.
 * @param picked the choice the learner tapped, or null if none yet.
 * @param correct the right answer.
 * @return the choice's look.
 */
fun optionState(option: String, picked: String?, correct: String): OptionState = when {
    picked == null -> OptionState.OPEN
    option == correct -> OptionState.CORRECT
    option == picked -> OptionState.WRONG
    else -> OptionState.DIMMED
}
