package dev.denlogv.lexislearned.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/** The Robolectric application context. */
fun testContext(): Context = ApplicationProvider.getApplicationContext()

/** A Room database that lives in memory and may be queried from the test thread. */
fun memoryDb(): AppDatabase = Room.inMemoryDatabaseBuilder(testContext(), AppDatabase::class.java).allowMainThreadQueries().build()
