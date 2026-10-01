package dev.denlogv.lexislearned.data

import androidx.room.Database
import androidx.room.RoomDatabase

/** The app's Room database. Changing the schema needs a version bump and a migration (see [Migrations]). */
@Database(
    entities = [DeckEntity::class, PartEntity::class, ChapterEntity::class, CardEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    /** Queries for decks, parts and chapters. */
    abstract fun structureDao(): StructureDao

    /** Queries for progress summaries. */
    abstract fun summaryDao(): SummaryDao

    /** Queries for cards and their progress. */
    abstract fun cardDao(): CardDao
}
