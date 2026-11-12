package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.SessionKeeper
import dev.denlogv.lexislearned.domain.Grade
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Coming back to a session after the deck, the words or the time have changed. */
class StudyResumeChangesTest : StudyResumeFixture() {
    @Test
    fun wordsThatWereDeletedAreDroppedFromTheSession() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 1)
        val planned = store.sessions.values.single().planner.plans.map { it.id }
        val gone = planned.first()
        db.openHelper.writableDatabase.execSQL("DELETE FROM cards WHERE id = $gone")
        val vm = open()
        assertTrue(vm.ui.value.offering)
        assertEquals(store.sessions.values.single().progress.totalWords - 1, vm.ui.value.totalWords)
        vm.continueSession()
        vm.ui.await { !it.loading && !it.offering }
        vm.playToEnd()
        assertEquals(3, vm.ui.value.completedWords)
    }

    @Test
    fun wordsThatWereCompletedElsewhereAreDroppedFromTheSession() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 1)
        val done = store.sessions.values.single().planner.plans.first().id
        study.finishSession(db.cardDao().cards(deckId).first { it.id == done }, Grade.GOOD, spaced = true)
        val vm = open()
        vm.continueSession()
        vm.ui.await { !it.loading && !it.offering }
        vm.playToEnd()
        assertEquals(3, vm.ui.value.completedWords)
        assertEquals(1, db.cardDao().cards(deckId).first { it.id == done }.sessionsDone)
    }

    @Test
    fun wordsThatWereResetCanStillBeContinued() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 1)
        store.sessions.values.single().planner.plans.forEach { library.resetCard(it.id) }
        val vm = open()
        vm.continueSession()
        vm.ui.await { !it.loading && !it.offering }
        vm.playToEnd()
        assertEquals(4, db.cardDao().cards(deckId).count { it.sessionsDone == 1 })
    }

    @Test
    fun aSessionWhoseDeckWasDeletedIsNotOfferedAndIsForgotten() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 2)
        library.delete(deckId)
        val vm = open()
        assertFalse(vm.ui.value.offering)
        assertTrue(vm.ui.value.finished)
        assertTrue(store.sessions.isEmpty())
    }

    @Test
    fun aReimportedDeckDoesNotInheritTheOldSession() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 2)
        library.delete(deckId)
        importDeck() // new ids, so the saved words are gone
        val vm = open()
        assertFalse(vm.ui.value.offering)
        assertEquals(4, vm.ui.value.totalWords)
    }

    @Test
    fun aVeryOldSessionIsNotOffered() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 2)
        now += SessionKeeper.MAX_AGE_MS + 1
        assertFalse(open().ui.value.offering)
    }

    @Test
    fun otherWordsDueNowDoNotChangeTheContinuedSession() = runBlocking {
        importDeck()
        val first = open()
        first.playAndLeave(steps = 2)
        val planned = store.sessions.values.single().planner.plans.map { it.id }.toSet()
        val second = open()
        second.continueSession()
        second.ui.await { !it.loading && !it.offering }
        second.playToEnd()
        val studied = db.cardDao().cards(deckId).filter { it.sessionsDone == 1 }.map { it.id }.toSet()
        assertTrue(studied.containsAll(planned))
        assertEquals(4, studied.size)
    }

    @Test
    fun aDamagedSavedPlanIsNotOffered() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 2)
        val saved = store.sessions.values.single()
        store.sessions[saved.scope] = saved.copy(planner = saved.planner.copy(plans = emptyList(), shown = null))
        val vm = open()
        assertFalse(vm.ui.value.offering)
        assertNotEquals(0, vm.ui.value.totalWords)
    }
}
