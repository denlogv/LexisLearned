package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.data.DeckSummary

/** An action on a deck that needs the user's confirmation. */
sealed interface DeckAction {
    /** The deck the action applies to. */
    val deck: DeckSummary

    /**
     * Clear the study progress of a deck.
     *
     * @property deck the deck.
     */
    data class Reset(override val deck: DeckSummary) : DeckAction

    /**
     * Delete a deck.
     *
     * @property deck the deck.
     */
    data class Delete(override val deck: DeckSummary) : DeckAction
}
