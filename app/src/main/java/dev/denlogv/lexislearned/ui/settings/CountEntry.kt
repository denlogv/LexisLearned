package dev.denlogv.lexislearned.ui.settings

/** What a number field makes of the text the learner typed. */
sealed interface CountEntry {
    /**
     * A usable number.
     *
     * @property value the number, inside the allowed range.
     */
    data class Accepted(val value: Int) : CountEntry

    /** Text that is not a whole number, or a number outside the allowed range; the setting keeps its last valid value. */
    data object Rejected : CountEntry
}

/**
 * Reads the text of a number field.
 *
 * Only plain digits count: an empty field, a sign, a decimal point, letters, spaces and numbers too large for an Int are all
 * rejected rather than guessed at, so no half-typed or pasted text can become a setting. A number outside [range] is rejected
 * too instead of being clamped, because clamping while the learner is still typing would rewrite "150" to "100" on the way to
 * "1500".
 *
 * @param text what is in the field.
 * @param range the allowed numbers.
 * @return [CountEntry.Accepted] with the number, or [CountEntry.Rejected].
 */
fun parseCount(text: String, range: IntRange): CountEntry {
    val number = text.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }?.toIntOrNull()
    return if (number != null && number in range) CountEntry.Accepted(number) else CountEntry.Rejected
}

/**
 * The line under a number field.
 *
 * @param range the allowed numbers.
 * @param rejected whether the text in the field is not usable.
 * @return what to type, and when the text is not usable, that the last valid number stays in use.
 */
fun countSupportText(range: IntRange, rejected: Boolean): String {
    val allowed = "whole number from ${range.first} to ${range.last}"
    return if (rejected) "Enter a $allowed. The last valid number stays in use." else "Any $allowed."
}
