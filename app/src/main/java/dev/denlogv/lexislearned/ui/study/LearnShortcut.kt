package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.domain.Grade

/** What a key does in a Learn or Check step. */
enum class LearnShortcut {
    /** Turns the card over. */
    FLIP,

    /** Rates the word "Forgot". */
    FORGOT,

    /** Rates the word "Got it". */
    GOT_IT,

    /** Rates the word "Easy". */
    EASY,

    /** "I know it": skips the word. */
    KNOWN,

    /** Ends the introduction of a word. */
    NEXT,
}

/**
 * Works out what a key does in a Learn or Check step. Space turns the card; Enter turns it when it still shows its front and
 * otherwise takes the middle choice ("Got it", or "Next" in an introduction); the number keys pick the buttons that are on
 * screen, in their order.
 *
 * @param key the key.
 * @param revealed whether the card was turned over at least once, which is when the buttons appear.
 * @param exposure whether the step only introduces the word, with "I know it" and "Next" instead of the rating.
 * @return what the key does, or null if it does nothing.
 */
fun learnShortcutFor(key: StepKey, revealed: Boolean, exposure: Boolean): LearnShortcut? = when {
    key == StepKey.Space -> LearnShortcut.FLIP
    key == StepKey.Confirm && !revealed -> LearnShortcut.FLIP
    key == StepKey.Confirm -> if (exposure) LearnShortcut.NEXT else LearnShortcut.GOT_IT
    key is StepKey.Number && revealed -> buttonShortcut(key.value, exposure)
    else -> null
}

/** The buttons of an introduction, in the order of their number keys. */
private val INTRODUCTION_BUTTONS = listOf(LearnShortcut.KNOWN, LearnShortcut.NEXT)

/** The rating buttons, in the order of their number keys. */
private val RATING_BUTTONS = listOf(LearnShortcut.FORGOT, LearnShortcut.GOT_IT, LearnShortcut.EASY)

/**
 * The button a number key picks.
 *
 * @param number the number on the key.
 * @param exposure whether the buttons are "I know it" and "Next" instead of the rating.
 * @return the button's shortcut, or null if there is no such button.
 */
private fun buttonShortcut(number: Int, exposure: Boolean): LearnShortcut? {
    val buttons = if (exposure) INTRODUCTION_BUTTONS else RATING_BUTTONS
    return buttons.getOrNull(number - 1)
}

/**
 * Carries out a shortcut.
 *
 * @receiver the shortcut.
 * @param flip turns the card over.
 * @param actions what the buttons do.
 */
fun LearnShortcut.perform(flip: () -> Unit, actions: StudyActions) = when (this) {
    LearnShortcut.FLIP -> flip()
    LearnShortcut.FORGOT -> actions.onGrade(Grade.AGAIN)
    LearnShortcut.GOT_IT -> actions.onGrade(Grade.GOOD)
    LearnShortcut.EASY -> actions.onGrade(Grade.EASY)
    LearnShortcut.KNOWN -> actions.onKnown()
    LearnShortcut.NEXT -> actions.onIntroduced()
}
