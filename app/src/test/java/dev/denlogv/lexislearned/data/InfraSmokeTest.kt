package dev.denlogv.lexislearned.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InfraSmokeTest {
    @Test
    fun roomRunsInMemoryUnderRobolectric() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val id = db.structureDao().insertDeck(DeckEntity(0, "s", "T", null, "en", "ru"))
        assertEquals("T", db.structureDao().deck(id)?.title)
        db.close()
    }
}
