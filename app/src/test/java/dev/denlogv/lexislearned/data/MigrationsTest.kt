package dev.denlogv.lexislearned.data

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationsTest {
    private val name = "migration-test.db"

    /** Creates a database the way version 1 of the app did and fills it with one studied and one new card. */
    private fun createVersion1() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(testContext()).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = createV1Tables(db)

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase.apply {
            execSQL("INSERT INTO decks VALUES (1, 'd', 'Deck', NULL, 'en', 'ru')")
            execSQL("INSERT INTO chapters VALUES (1, 1, 'c', 'Chapter', NULL)")
            execSQL("INSERT INTO cards VALUES (1, 1, 1, 'a', 'w', NULL, NULL, NULL, 't', NULL, NULL, 3, 2.5, 6, 99, 0)")
            execSQL("INSERT INTO cards VALUES (2, 1, 1, 'b', 'x', NULL, NULL, NULL, 'u', NULL, NULL, 0, 2.5, 0, NULL, 0)")
        }
        helper.close()
    }

    private fun createV1Tables(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `decks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourceId` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `nativeTitle` TEXT, `frontLang` TEXT NOT NULL, `backLang` TEXT NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE `chapters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `deckId` INTEGER NOT NULL, " +
                "`sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `nativeTitle` TEXT, " +
                "FOREIGN KEY(`deckId`) REFERENCES `decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX `index_chapters_deckId` ON `chapters` (`deckId`)")
        db.execSQL(
            "CREATE TABLE `cards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `deckId` INTEGER NOT NULL, " +
                "`chapterId` INTEGER NOT NULL, `sourceId` TEXT NOT NULL, `frontText` TEXT NOT NULL, `frontTranscription` TEXT, " +
                "`frontExample` TEXT, `frontGender` TEXT, `backText` TEXT NOT NULL, `backExample` TEXT, `backGender` TEXT, " +
                "`reps` INTEGER NOT NULL, `ease` REAL NOT NULL, `intervalDays` INTEGER NOT NULL, `dueAt` INTEGER, " +
                "`lapses` INTEGER NOT NULL, FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX `index_cards_deckId` ON `cards` (`deckId`)")
        db.execSQL("CREATE INDEX `index_cards_chapterId` ON `cards` (`chapterId`)")
        db.execSQL("CREATE INDEX `index_cards_dueAt` ON `cards` (`dueAt`)")
    }

    @Test
    fun version1DatabaseUpgradesAndKeepsProgress() = runBlocking {
        createVersion1()
        val db = Room.databaseBuilder(testContext(), AppDatabase::class.java, name)
            .addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        val cards = db.cardDao().cards(1)
        assertEquals(listOf(3, 0), cards.map { it.sessionsDone })
        assertEquals(99L, cards[0].dueAt)
        assertNull(db.structureDao().chapters(1).single().partId)
        assertEquals(emptyList<PartEntity>(), db.structureDao().partsList(1))
        db.close()
    }
}
