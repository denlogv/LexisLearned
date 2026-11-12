package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.SessionScope
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.StudyMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Leaving a session unfinished and coming back to it. */
class StudyResumeTest : StudyResumeFixture() {
    @Test
    fun anUnfinishedSessionIsOfferedWhenTheSameScopeIsOpenedAgain() = runBlocking {
        importDeck()
        val first = open()
        first.playAndLeave()
        val left = first.ui.value
        val second = open()
        val offer = second.ui.value
        assertTrue(offer.offering)
        assertEquals(left.stepsLeft, offer.stepsLeft)
        assertEquals(left.totalWords, offer.totalWords)
        assertEquals(left.completedWords, offer.completedWords)
        assertTrue(offer.current.isEmpty())
    }

    @Test
    fun continuingRestoresTheStepTheCountersAndThePlan() = runBlocking {
        importDeck()
        val first = open()
        first.playAndLeave()
        val left = first.ui.value
        val second = open()
        second.continueSession()
        val back = second.ui.await { !it.loading && !it.offering }
        assertEquals(left.mode, back.mode)
        assertEquals(left.current.map { it.id }, back.current.map { it.id })
        assertEquals(left.exposure, back.exposure)
        assertEquals(
            listOf(left.stepsDone, left.stepsLeft, left.correct, left.wrong),
            listOf(back.stepsDone, back.stepsLeft, back.correct, back.wrong),
        )
        assertEquals(listOf(left.completedWords, left.totalWords), listOf(back.completedWords, back.totalWords))
        assertTrue(back.wrong > 0)
    }

    @Test
    fun aContinuedSessionRunsToTheEndAndSavesEveryWordOnce() = runBlocking {
        importDeck()
        val first = open()
        first.playAndLeave(steps = 12)
        val savedBefore = db.cardDao().cards(deckId).count { it.sessionsDone == 1 }
        val second = open()
        second.continueSession()
        second.ui.await { !it.loading && !it.offering }
        second.playToEnd()
        assertEquals(4, second.ui.value.completedWords)
        assertEquals(4, db.cardDao().cards(deckId).count { it.sessionsDone == 1 })
        assertEquals(0, db.cardDao().cards(deckId).count { it.sessionsDone > 1 })
        assertTrue(savedBefore <= 4)
    }

    @Test
    fun aFinishedSessionLeavesNothingBehind() = runBlocking {
        importDeck()
        val vm = open()
        vm.playToEnd()
        assertTrue(store.sessions.isEmpty())
        assertFalse(open().ui.value.offering)
    }

    @Test
    fun anUnfinishedSessionIsKeptAfterEveryStep() = runBlocking {
        importDeck()
        val vm = open()
        val writes = store.writes
        vm.playAndLeave(steps = 2)
        assertEquals(writes + 2, store.writes)
        assertEquals(1, store.sessions.size)
    }

    @Test
    fun startingANewSessionThrowsTheOldOneAway() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 2)
        val vm = open()
        vm.startNewSession()
        val fresh = vm.ui.await { !it.loading && !it.offering }
        assertEquals(0, fresh.stepsDone)
        assertEquals(0, fresh.wrong)
        assertEquals(4, fresh.totalWords)
        assertEquals(0, store.sessions.values.single().progress.stepsDone)
    }

    @Test
    fun theNewSessionReplacesTheOldOneEvenIfTheScreenIsLeftAtOnce() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 3)
        val vm = open()
        store.failing = true // the new session cannot be kept, so only the discard shows
        vm.startNewSession()
        vm.ui.await { !it.loading && !it.offering }
        assertTrue(store.sessions.isEmpty())
    }

    @Test
    fun theSessionOfAnotherChapterIsNotOffered() = runBlocking {
        importDeck()
        open(chapterId = chapter(0)).playAndLeave(steps = 2)
        assertFalse(open(chapterId = chapter(1)).ui.value.offering)
        assertFalse(open().ui.value.offering)
        assertTrue(open(chapterId = chapter(0)).ui.value.offering)
    }

    @Test
    fun thePartSessionAndTheBookSessionAreSeparate() = runBlocking {
        importDeck()
        val part = SessionScope(deckId, partId = 7)
        open().playAndLeave(steps = 2)
        assertEquals(setOf(SessionScope(deckId)), store.sessions.keys)
        assertFalse(open(partId = part.partId!!).ui.value.offering)
    }

    @Test
    fun theSettingsTheSessionStartedWithAreUsedWhenItContinues() = runBlocking {
        importDeck()
        val first = open()
        first.playAndLeave(steps = 2)
        settings.setDirection(Direction.REVERSE)
        StudyMode.entries.filter { it != StudyMode.SELECT }.forEach(settings::toggleMode)
        val second = open()
        second.continueSession()
        val back = second.ui.await { !it.loading && !it.offering }
        assertEquals(Direction.FORWARD, back.direction)
        second.playToEnd()
        assertEquals(4, second.ui.value.completedWords)
    }

    @Test
    fun aNewSessionUsesTheCurrentSettings() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 2)
        settings.setDirection(Direction.REVERSE)
        val vm = open()
        vm.startNewSession()
        assertEquals(Direction.REVERSE, vm.ui.await { !it.loading && !it.offering }.direction)
    }

    @Test
    fun answersWhileTheOfferIsShownDoNothing() = runBlocking {
        importDeck()
        open().playAndLeave(steps = 2)
        val vm = open()
        val before = vm.ui.value
        vm.answer(Grade.GOOD)
        vm.answerPair(emptySet())
        vm.introduced()
        vm.known()
        assertEquals(before, vm.ui.value)
    }

    @Test
    fun theOfferCanOnlyBeAnsweredOnce() = runBlocking {
        importDeck()
        val running = open()
        running.continueSession() // nothing is on offer
        running.startNewSession()
        assertFalse(running.ui.value.offering)
        running.playAndLeave(steps = 1)
        val vm = open()
        vm.continueSession()
        vm.ui.await { !it.loading && !it.offering }
        val shown = vm.ui.value
        vm.continueSession()
        vm.startNewSession()
        assertEquals(shown, vm.ui.value)
    }

    @Test
    fun aFullDiskOnlyMeansNothingIsOffered() = runBlocking {
        importDeck()
        store.failing = true
        val vm = open()
        vm.playAndLeave(steps = 3)
        assertEquals(3, vm.ui.value.stepsDone)
        assertFalse(open().ui.value.offering)
    }
}
