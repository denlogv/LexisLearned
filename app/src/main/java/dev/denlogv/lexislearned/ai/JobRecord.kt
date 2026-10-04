package dev.denlogv.lexislearned.ai

import kotlinx.serialization.Serializable

/**
 * What must survive a restart of the app to resume a generation that is not finished: which sections are still to do, and the deck
 * they are added to. The book itself is stored next to it (see [JobStore]).
 *
 * @property deckId the stored deck the finished sections went into, or null if no chapter was stored yet.
 * @property cards number of cards in that deck.
 * @property remaining indexes of the sections that are not in the deck yet: failed, interrupted or not reached.
 * @property failed descriptions of the sections that failed, to show again after a restart.
 * @property sourceLang the book's language code chosen for this run.
 * @property cardsPer1000Words how many cards were asked for per thousand words.
 */
@Serializable
data class JobRecord(
    val deckId: Long?,
    val cards: Int,
    val remaining: Set<Int>,
    val failed: List<String>,
    val sourceLang: String,
    val cardsPer1000Words: Int,
)

/**
 * A job as it was found on disk.
 *
 * @property record the progress of the job.
 * @property epub the contents of the book's EPUB file.
 */
class StoredJob(val record: JobRecord, val epub: ByteArray)
