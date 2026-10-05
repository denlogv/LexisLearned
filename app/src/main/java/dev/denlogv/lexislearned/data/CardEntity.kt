package dev.denlogv.lexislearned.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.denlogv.lexislearned.domain.CardProgress
import dev.denlogv.lexislearned.domain.Direction

/**
 * A row of the `cards` table: one word or phrase with its study progress.
 *
 * @property id database id; cards are shown in id order, which is reading order.
 * @property deckId the deck the card belongs to.
 * @property chapterId the chapter the card belongs to.
 * @property sourceId the card's id in its source file; progress is matched by it on re-import.
 * @property frontText the word in the learned language.
 * @property frontTranscription pronunciation of the word.
 * @property frontExample example sentence in the learned language.
 * @property frontGender article marker of the word.
 * @property backText the translation.
 * @property backExample example sentence in the native language.
 * @property backGender article marker of the translation.
 * @property reps consecutive successful reviews since the last lapse.
 * @property ease SM-2 ease factor.
 * @property intervalDays current review interval in days.
 * @property dueAt epoch milliseconds when the card is due, or null if never studied.
 * @property lapses how often the card was failed after having been learned.
 * @property sessionsDone sessions in which the word went through all selected modes successfully.
 */
@Entity(
    tableName = "cards",
    foreignKeys = [ForeignKey(ChapterEntity::class, ["id"], ["chapterId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("deckId"), Index("chapterId"), Index("dueAt")],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val chapterId: Long,
    val sourceId: String,
    val frontText: String,
    val frontTranscription: String?,
    val frontExample: String?,
    val frontGender: String?,
    val backText: String,
    val backExample: String?,
    val backGender: String?,
    val reps: Int = 0,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val dueAt: Long? = null,
    val lapses: Int = 0,
    @ColumnInfo(defaultValue = "0") val sessionsDone: Int = 0,
) {
    /** The card's study state as a domain object. */
    val progress: CardProgress get() = CardProgress(reps, ease, intervalDays, dueAt, lapses, sessionsDone)

    /**
     * The text to show as the question.
     *
     * @param d which side is the question.
     * @return the front text when studying forward, else the back text.
     */
    fun prompt(d: Direction): String = if (d == Direction.FORWARD) frontText else backText

    /**
     * The expected answer.
     *
     * @param d which side is the question.
     * @return the back text when studying forward, else the front text.
     */
    fun answer(d: Direction): String = if (d == Direction.FORWARD) backText else frontText

    /**
     * A hint to show with the question: the pronunciation, when the question is the foreign word.
     *
     * @param d which side is the question.
     * @return the transcription when studying forward, otherwise null.
     */
    fun promptHint(d: Direction): String? = if (d == Direction.FORWARD) frontTranscription else null

    /**
     * The example sentence on the question side.
     *
     * @param d which side is the question.
     * @return the example in the question's language, or null if there is none.
     */
    fun promptExample(d: Direction): String? = if (d == Direction.FORWARD) frontExample else backExample

    /**
     * The example sentence on the answer side.
     *
     * @param d which side is the question.
     * @return the example in the answer's language, or null if there is none.
     */
    fun answerExample(d: Direction): String? = if (d == Direction.FORWARD) backExample else frontExample

    /**
     * The example sentences to show once the answer is revealed: the original sentence in the question's language first,
     * then its translation.
     *
     * @param d which side is the question.
     * @return the sentences that exist; empty if the card has none.
     */
    fun revealedExamples(d: Direction): List<String> = listOfNotNull(promptExample(d), answerExample(d))

    /**
     * A hint to show with the answer: the pronunciation, when the answer is the foreign word.
     *
     * @param d which side is the question.
     * @return the transcription when studying in reverse, otherwise null.
     */
    fun answerHint(d: Direction): String? = if (d == Direction.FORWARD) null else frontTranscription
}
