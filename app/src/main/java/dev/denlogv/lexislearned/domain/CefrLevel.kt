package dev.denlogv.lexislearned.domain

/**
 * The learner's level on the CEFR scale. Cards are generated for vocabulary above this level.
 *
 * @property label short name such as "B1".
 * @property title descriptive name such as "Intermediate".
 */
enum class CefrLevel(val label: String, val title: String) {
    /** Beginner. */
    A1("A1", "Beginner"),

    /** Elementary. */
    A2("A2", "Elementary"),

    /** Intermediate. */
    B1("B1", "Intermediate"),

    /** Upper intermediate. */
    B2("B2", "Upper intermediate"),

    /** Advanced. */
    C1("C1", "Advanced"),

    /** Proficient. */
    C2("C2", "Proficient"),
}
