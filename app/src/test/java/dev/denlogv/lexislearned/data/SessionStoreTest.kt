package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.StudyMode
import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private val dir get() = File(folder.root, "sessions")

    @Test
    fun aStoredSessionComesBackAsItWas() {
        val saved = savedSession()
        FileSessionStore(dir).save(saved)
        assertEquals(saved, FileSessionStore(dir).load(chapterScope)) // a new object, as after a restart
    }

    @Test
    fun theLatestSessionWinsAndLeavesNoTemporaryFiles() {
        val store = FileSessionStore(dir)
        store.save(savedSession(savedAt = 1))
        store.save(savedSession(savedAt = 2))
        assertEquals(2L, store.load(chapterScope)!!.savedAt)
        assertEquals(1, dir.list()!!.size)
    }

    @Test
    fun everyScopeHasItsOwnSession() {
        val store = FileSessionStore(dir)
        val book = SessionScope(1)
        val part = SessionScope(1, partId = 2)
        val other = SessionScope(1, chapterId = 6)
        listOf(chapterScope, book, part).forEach { store.save(savedSession(it)) }
        assertEquals(chapterScope, store.load(chapterScope)!!.scope)
        assertEquals(book, store.load(book)!!.scope)
        assertEquals(part, store.load(part)!!.scope)
        assertNull(store.load(other))
        assertNull(store.load(SessionScope(2, chapterId = 5)))
    }

    @Test
    fun scopesGetDifferentKeys() {
        val keys = listOf(SessionScope(1), SessionScope(1, 2), SessionScope(1, partId = 2), SessionScope(2), SessionScope(12)).map {
            it.key
        }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun clearingForgetsOnlyThatScope() {
        val store = FileSessionStore(dir)
        store.save(savedSession(chapterScope))
        store.save(savedSession(SessionScope(1)))
        store.clear(chapterScope)
        store.clear(chapterScope) // nothing to forget is fine
        assertNull(store.load(chapterScope))
        assertEquals(SessionScope(1), store.load(SessionScope(1))!!.scope)
    }

    @Test
    fun nothingIsFoundBeforeAnythingWasStored() {
        assertNull(FileSessionStore(dir).load(chapterScope))
    }

    @Test
    fun aDamagedFileIsNotASession() {
        val store = FileSessionStore(dir)
        store.save(savedSession())
        File(dir, "session-${chapterScope.key}.json").writeText("{not json")
        assertNull(store.load(chapterScope))
    }

    @Test
    fun aFileFromAnotherFormatIsNotASession() {
        val store = FileSessionStore(dir)
        dir.mkdirs()
        File(dir, "session-${chapterScope.key}.json").writeText("""{"scope":{"deckId":1},"unrelated":true}""")
        assertNull(store.load(chapterScope))
    }

    @Test
    fun aFieldThatIsNoLongerWrittenStillLoads() {
        val store = FileSessionStore(dir)
        store.save(savedSession())
        val file = File(dir, "session-${chapterScope.key}.json")
        file.writeText(file.readText().replaceFirst("{", """{"retiredField":1,"""))
        assertEquals(savedSession(), store.load(chapterScope))
    }

    @Test(expected = IOException::class)
    fun aDirectoryThatCannotBeCreatedFails() {
        FileSessionStore(File(folder.newFile("blocker"), "sessions")).save(savedSession())
    }

    @Test
    fun sessionSettingsFollowThePrefs() {
        val prefs = Prefs(
            direction = Direction.REVERSE,
            modes = setOf(StudyMode.PAIR),
            rounds = 3,
            pairSize = 5,
            sessionsToComplete = 2,
            spaceSessions = false,
        )
        assertEquals(SessionSettings(Direction.REVERSE, setOf(StudyMode.PAIR), 3, 5, 2, false), SessionSettings.of(prefs))
        assertNotEquals(SessionSettings.of(prefs), SessionSettings.of(Prefs()))
    }
}
