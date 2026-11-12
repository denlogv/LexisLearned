package dev.denlogv.lexislearned.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The whole-chapter switch: its default, the learner's explicit choice and the one-time suggestion to switch it on. */
@RunWith(RobolectricTestRunner::class)
class WholeChapterChoiceTest {
    @Test
    fun wholeChapterIsOnForSettingsThatNeverStoredANumber() {
        assertTrue(settingsOver(preparedPrefs("rounds" to 2)).prefs.value.wholeChapter)
    }

    @Test
    fun wholeChapterIsOffForALearnerWhoChoseTheCardsOrTheNewCards() {
        assertFalse(settingsOver(preparedPrefs("sessionSize" to 10)).prefs.value.wholeChapter)
        assertFalse(settingsOver(preparedPrefs("newPerSession" to 5)).prefs.value.wholeChapter)
        assertFalse(settingsOver(preparedPrefs("sessionSize" to 12, "newPerSession" to 3)).prefs.value.wholeChapter)
    }

    @Test
    fun anExplicitChoiceBeatsTheDefaultInBothDirections() {
        val kept = preparedPrefs("sessionSize" to 10)
        settingsOver(kept).setWholeChapter(true)
        assertTrue(settingsOver(kept).prefs.value.wholeChapter)
        val fresh = preparedPrefs()
        settingsOver(fresh).setWholeChapter(false)
        assertFalse(settingsOver(fresh).prefs.value.wholeChapter)
    }

    @Test
    fun theDefaultIsSettledOnceSoLaterNumberChangesDoNotFlipIt() {
        val sp = preparedPrefs()
        val fresh = settingsOver(sp)
        fresh.setSessionSize(7)
        fresh.setNewPerSession(2)
        assertTrue(fresh.prefs.value.wholeChapter)
        assertTrue(settingsOver(sp).prefs.value.wholeChapter)
        assertEquals(7, settingsOver(sp).prefs.value.sessionSize)
    }

    @Test
    fun wholeChapterCanBeSwitchedAndIsRemembered() {
        val sp = preparedPrefs()
        settingsOver(sp).setWholeChapter(false)
        assertFalse(settingsOver(sp).prefs.value.wholeChapter)
        val again = settingsOver(sp)
        again.setWholeChapter(true)
        assertTrue(settingsOver(sp).prefs.value.wholeChapter)
    }

    @Test
    fun theSuggestionIsForALearnerWhoseSwitchIsOffOnlyBecauseOfTheirNumbers() {
        assertTrue(settingsOver(preparedPrefs("sessionSize" to 10)).prefs.value.suggestWholeChapter)
        assertTrue(settingsOver(preparedPrefs("newPerSession" to 5)).prefs.value.suggestWholeChapter)
    }

    @Test
    fun noSuggestionOnAFreshInstallEvenAfterTheNumbersAreSet() {
        val sp = preparedPrefs()
        val fresh = settingsOver(sp)
        assertFalse(fresh.prefs.value.suggestWholeChapter)
        fresh.setSessionSize(20)
        assertFalse(fresh.prefs.value.suggestWholeChapter)
        assertFalse(settingsOver(sp).prefs.value.suggestWholeChapter)
    }

    @Test
    fun noSuggestionOnceTheSwitchWasSetInEitherDirection() {
        val on = preparedPrefs("sessionSize" to 10)
        settingsOver(on).setWholeChapter(true)
        assertFalse(settingsOver(on).prefs.value.suggestWholeChapter)
        val off = preparedPrefs("sessionSize" to 10)
        settingsOver(off).setWholeChapter(false)
        assertFalse(settingsOver(off).prefs.value.suggestWholeChapter)
        assertFalse(settingsOver(off).prefs.value.wholeChapter)
    }

    @Test
    fun theSuggestionIsShownUntilItIsSeenAndThenNeverAgainAcrossRestarts() {
        val sp = preparedPrefs("sessionSize" to 10)
        val first = settingsOver(sp)
        assertTrue(first.prefs.value.suggestWholeChapter)
        assertTrue(settingsOver(sp).prefs.value.suggestWholeChapter) // not seen yet: it comes back after a restart
        first.markWholeChapterSuggestionSeen()
        assertFalse(first.prefs.value.suggestWholeChapter)
        val restarted = settingsOver(sp).prefs.value
        assertFalse(restarted.suggestWholeChapter)
        assertFalse(restarted.wholeChapter) // seeing it does not change the switch
        assertEquals(10, restarted.sessionSize)
    }

    @Test
    fun aSwitchValueFromAnEarlierTestBuildIsNotTakenForAChoice() {
        val p = settingsOver(preparedPrefs("sessionSize" to 10, "wholeChapter" to false)).prefs.value
        assertTrue(p.suggestWholeChapter)
    }
}
