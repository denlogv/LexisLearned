package dev.denlogv.lexislearned.domain

import kotlinx.serialization.Serializable

/**
 * One side of a card: a word or phrase with optional extras.
 *
 * @property text the word or phrase itself.
 * @property transcription pronunciation (IPA), usually only on the side in the learned language.
 * @property example an example sentence using the word.
 * @property gender optional grammatical marker such as an article; kept as given and not interpreted by the app.
 */
@Serializable
data class Side(val text: String, val transcription: String? = null, val example: String? = null, val gender: String? = null)

/**
 * Spaced-repetition and session-counting state of one card.
 *
 * @property reps consecutive successful reviews since the last lapse.
 * @property ease SM-2 ease factor; higher means longer intervals.
 * @property intervalDays current interval until the next review, in days.
 * @property dueAt epoch milliseconds when the card is due, or null if it was never studied.
 * @property lapses how often the card was failed after having been learned.
 * @property sessionsDone sessions in which the word went through all selected modes successfully.
 */
@Serializable
data class CardProgress(
    val reps: Int = 0,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val dueAt: Long? = null,
    val lapses: Int = 0,
    val sessionsDone: Int = 0,
)

/**
 * A flashcard.
 *
 * @property id stable identifier, used to keep progress when a deck is imported again.
 * @property front the side in the language being learned.
 * @property back the side in the learner's native language.
 * @property progress the card's study state.
 */
@Serializable
data class Card(val id: String, val front: Side, val back: Side, val progress: CardProgress = CardProgress())

/**
 * A group of chapters, such as a novel in an omnibus or a part of a book. Decks without parts have none.
 *
 * @property id stable identifier.
 * @property title title in the learned language.
 * @property nativeTitle title in the native language, if known.
 */
@Serializable
data class PartRef(val id: String, val title: String, val nativeTitle: String? = null)

/**
 * A chapter: a named list of cards, optionally belonging to a part.
 *
 * @property id stable identifier.
 * @property title title in the learned language.
 * @property nativeTitle title in the native language, if known.
 * @property cards the chapter's cards in reading order.
 * @property part the part this chapter belongs to, or null if it stands alone.
 */
@Serializable
data class Chapter(val id: String, val title: String, val nativeTitle: String? = null, val cards: List<Card>, val part: PartRef? = null)

/**
 * A deck: one book's vocabulary, organised in chapters (and optionally parts).
 *
 * @property id stable identifier; importing a deck with the same id replaces the old one but keeps progress.
 * @property title title in the learned language.
 * @property nativeTitle title in the native language, if known.
 * @property chapters all chapters in reading order.
 * @property frontLang language of the learned side (front), ISO 639-1.
 * @property backLang language of the native side (back), ISO 639-1.
 */
@Serializable
data class Deck(
    val id: String,
    val title: String,
    val nativeTitle: String? = null,
    val chapters: List<Chapter>,
    val frontLang: String = "en",
    val backLang: String = "ru",
) {
    /** Total number of cards in all chapters. */
    val cardCount: Int get() = chapters.sumOf { it.cards.size }
}

/** The ways of studying a word; each selected mode is used for every word in a session. */
enum class StudyMode {
    /** Flip a card and see the word with examples. */
    LEARN,

    /** Match words with their translations. */
    PAIR,

    /** Pick the right answer from several. */
    SELECT,

    /** Recall the answer, reveal it, rate yourself. */
    CHECK,

    /** Type the answer. */
    TYPE,
}

/** Which side of a card is shown as the question. */
enum class Direction {
    /** The front (learned language) is the prompt. */
    FORWARD,

    /** The back (native language) is the prompt. */
    REVERSE,
}
