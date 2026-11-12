package dev.denlogv.lexislearned.data

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionKeeperTest {
    private val store = MemorySessionStore()
    private var now = 10_000L
    private val keeper = SessionKeeper(store, { now })

    private suspend fun keep(scope: SessionScope = chapterScope) = keeper.keep(scope, sessionSettings, sessionProgress, plannerSnapshot())

    @Test
    fun aKeptSessionIsFoundWithTheTimeItWasKept() = runBlocking {
        keep()
        val found = keeper.find(chapterScope)!!
        assertEquals(10_000L, found.savedAt)
        assertEquals(sessionSettings, found.settings)
        assertEquals(sessionProgress, found.progress)
        assertEquals(plannerSnapshot(), found.planner)
    }

    @Test
    fun nothingIsFoundForAScopeThatWasNotKept() = runBlocking {
        keep()
        assertNull(keeper.find(SessionScope(1, chapterId = 6)))
        assertNull(keeper.find(SessionScope(1)))
    }

    @Test
    fun aSessionAtTheAgeLimitIsStillOffered() = runBlocking {
        keep()
        now += SessionKeeper.MAX_AGE_MS
        assertEquals(10_000L, keeper.find(chapterScope)!!.savedAt)
    }

    @Test
    fun aVeryOldSessionIsNotOfferedAndIsForgotten() = runBlocking {
        keep()
        now += SessionKeeper.MAX_AGE_MS + 1
        assertNull(keeper.find(chapterScope))
        assertTrue(store.sessions.isEmpty())
    }

    @Test
    fun aSessionFromTheFutureIsNotTooOld() = runBlocking {
        keep()
        now -= 5_000
        assertEquals(10_000L, keeper.find(chapterScope)!!.savedAt)
    }

    @Test
    fun aSessionStoredUnderTheWrongScopeIsIgnored() = runBlocking {
        store.sessions[SessionScope(1, chapterId = 6)] = savedSession(chapterScope, savedAt = now)
        assertNull(keeper.find(SessionScope(1, chapterId = 6)))
        assertTrue(store.sessions.isEmpty())
    }

    @Test
    fun discardingForgetsTheSessionAndComesAfterEarlierWrites() = runBlocking {
        repeat(20) { keep() }
        keeper.discard(chapterScope)
        assertNull(keeper.find(chapterScope))
    }

    @Test
    fun aFullDiskIsNotAnError() = runBlocking {
        store.failing = true
        keep()
        assertNull(keeper.find(chapterScope))
        keeper.discard(chapterScope)
    }

    @Test
    fun aKeepAfterACancelledCallerStillCompletes() = runBlocking {
        val job = launch(start = CoroutineStart.UNDISPATCHED) { keep() }
        job.cancel()
        job.join()
        assertEquals(1, store.writes)
    }
}
