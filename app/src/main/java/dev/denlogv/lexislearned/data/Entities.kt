package dev.denlogv.lexislearned.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A row of the `decks` table: one book.
 *
 * @property id database id.
 * @property sourceId the deck's id in its source file; importing the same id again replaces the deck.
 * @property title title in the learned language.
 * @property nativeTitle title in the native language, if known.
 * @property frontLang language of the learned side (ISO 639-1).
 * @property backLang language of the native side (ISO 639-1).
 */
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val title: String,
    val nativeTitle: String?,
    val frontLang: String,
    val backLang: String,
)

/**
 * A row of the `parts` table: a group of chapters.
 *
 * @property id database id.
 * @property deckId the deck the part belongs to.
 * @property sourceId the part's id in its source file.
 * @property title title in the learned language.
 * @property nativeTitle title in the native language, if known.
 */
@Entity(
    tableName = "parts",
    foreignKeys = [ForeignKey(DeckEntity::class, ["id"], ["deckId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("deckId")],
)
data class PartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val sourceId: String,
    val title: String,
    val nativeTitle: String?,
)

/**
 * A row of the `chapters` table.
 *
 * @property id database id; chapters are shown in id order, which is reading order.
 * @property deckId the deck the chapter belongs to.
 * @property sourceId the chapter's id in its source file.
 * @property title title in the learned language.
 * @property nativeTitle title in the native language, if known.
 * @property partId the part the chapter belongs to, or null if it stands alone.
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(DeckEntity::class, ["id"], ["deckId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("deckId"), Index("partId")],
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val sourceId: String,
    val title: String,
    val nativeTitle: String?,
    val partId: Long? = null,
)
