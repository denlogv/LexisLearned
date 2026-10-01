package dev.denlogv.lexislearned

import android.app.Application
import android.content.Context
import androidx.room.Room
import dev.denlogv.lexislearned.ai.GenerationManager
import dev.denlogv.lexislearned.data.AppDatabase
import dev.denlogv.lexislearned.data.DeckRepository
import dev.denlogv.lexislearned.data.Migrations
import dev.denlogv.lexislearned.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The application: creates the long-lived objects (settings, database repository, generation manager) once. */
class LexisLearnedApp : Application() {
    /** Scope for work that must outlive any screen, such as generating a deck. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The user's settings. */
    lateinit var settings: Settings
        private set

    /** Access to stored decks. */
    lateinit var repository: DeckRepository
        private set

    /** Creates decks from EPUBs. */
    lateinit var generation: GenerationManager
        private set

    /** Creates the settings, the database and the objects built on them. */
    override fun onCreate() {
        super.onCreate()
        settings = Settings(getSharedPreferences("settings", Context.MODE_PRIVATE))
        @Suppress("SpreadOperator") // Runs once at startup and the array has a handful of entries.
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "lexislearned.db")
            .addMigrations(*Migrations.ALL)
            .build()
        repository = DeckRepository(db)
        generation = GenerationManager(appScope, repository, settings)
    }
}
