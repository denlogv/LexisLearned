package dev.denlogv.lexislearned.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Database migrations. Users' decks and progress must survive every update. */
object Migrations {
    /** Version 1 to 2: adds parts, a level between deck and chapter. Existing decks keep working without parts. */
    val V1_TO_V2: Migration = object : Migration(1, 2) {
        /**
         * Creates the `parts` table and links chapters to it.
         *
         * @param db the database to migrate.
         */
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `parts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`deckId` INTEGER NOT NULL, `sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `nativeTitle` TEXT, " +
                    "FOREIGN KEY(`deckId`) REFERENCES `decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_parts_deckId` ON `parts` (`deckId`)")
            db.execSQL("ALTER TABLE `chapters` ADD COLUMN `partId` INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_partId` ON `chapters` (`partId`)")
        }
    }

    /** Version 2 to 3: counts finished sessions per word. Earlier reviews become that many sessions. */
    val V2_TO_V3: Migration = object : Migration(2, 3) {
        /**
         * Adds the session counter and fills it from the review count.
         *
         * @param db the database to migrate.
         */
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `cards` ADD COLUMN `sessionsDone` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `cards` SET `sessionsDone` = `reps` WHERE `reps` > 0")
        }
    }

    /** All migrations, in order. */
    val ALL: Array<Migration> = arrayOf(V1_TO_V2, V2_TO_V3)
}
