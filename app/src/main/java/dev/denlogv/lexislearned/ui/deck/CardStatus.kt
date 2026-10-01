package dev.denlogv.lexislearned.ui.deck

import dev.denlogv.lexislearned.data.CardEntity

/**
 * How far a word has come, as shown in the word list.
 *
 * @property label the text to show, such as "new", "1/3 sessions" or "completed".
 * @property due true if the word is due for review, which is highlighted.
 */
data class CardStatus(val label: String, val due: Boolean)

/**
 * Works out the status of a word.
 *
 * @param card the word.
 * @param now the current time in epoch milliseconds.
 * @param needed the number of sessions that completes a word.
 * @return the word's status.
 */
fun cardStatus(card: CardEntity, now: Long, needed: Int): CardStatus = when {
    card.sessionsDone >= needed -> CardStatus("completed", false)
    card.dueAt != null && card.dueAt <= now -> CardStatus("due · ${card.sessionsDone}/$needed", true)
    card.sessionsDone > 0 -> CardStatus("${card.sessionsDone}/$needed ${if (needed == 1) "session" else "sessions"}", false)
    card.dueAt != null -> CardStatus("learning", false)
    else -> CardStatus("new", false)
}

/**
 * Whether a word has any study progress to reset.
 *
 * @param card the word.
 * @return true if the word was studied at least once.
 */
fun hasProgress(card: CardEntity): Boolean = card.sessionsDone > 0 || card.dueAt != null
