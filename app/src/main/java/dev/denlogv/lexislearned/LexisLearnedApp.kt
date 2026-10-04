package dev.denlogv.lexislearned

import android.app.Application
import android.content.Context
import androidx.room.Room
import dev.denlogv.lexislearned.ai.FileJobStore
import dev.denlogv.lexislearned.ai.GenerationManager
import dev.denlogv.lexislearned.data.AppDatabase
import dev.denlogv.lexislearned.data.DeckLibrary
import dev.denlogv.lexislearned.data.DeckStorage
import dev.denlogv.lexislearned.data.Migrations
import dev.denlogv.lexislearned.data.Settings
import dev.denlogv.lexislearned.data.StudyRepository
import dev.denlogv.lexislearned.service.NoticeCleaner
import dev.denlogv.lexislearned.service.NoticeNotifier
import dev.denlogv.lexislearned.service.ServiceKeepAlive
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The application: creates the long-lived objects (settings, database, generation manager) once and restores a paused generation. */
class LexisLearnedApp : Application() {
    /** Scope for work that must outlive any screen, such as generating a deck. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The user's settings. */
    lateinit var settings: Settings
        private set

    /** The stored decks, to browse and manage. */
    lateinit var library: DeckLibrary
        private set

    /** What study sessions read and write. */
    lateinit var study: StudyRepository
        private set

    /** Importing and exporting whole decks. */
    lateinit var storage: DeckStorage
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
        library = DeckLibrary(db)
        study = StudyRepository(db)
        storage = DeckStorage(db)
        generation = GenerationManager(appScope, storage, settings, FileJobStore(File(filesDir, "generation")), ServiceKeepAlive(this))
        NoticeCleaner(generation.state, appScope, NoticeNotifier(this)::cancel).start()
        generation.restore()
    }
}
