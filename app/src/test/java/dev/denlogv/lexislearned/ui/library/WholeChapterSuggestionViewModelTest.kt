package dev.denlogv.lexislearned.ui.library

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.preparedPrefs
import dev.denlogv.lexislearned.data.settingsOver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WholeChapterSuggestionViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val kept = preparedPrefs("sessionSize" to 12, "newPerSession" to 4)

    @Test
    fun theOfferIsOnScreenForALearnerWhoKeptTheirNumbers() {
        assertTrue(WholeChapterSuggestionViewModel(settingsOver(kept)).visible.value)
    }

    @Test
    fun noOfferOnAFreshInstall() {
        assertFalse(WholeChapterSuggestionViewModel(settingsOver(preparedPrefs())).visible.value)
    }

    @Test
    fun noOfferForSomeoneWhoDecidedOnTheSwitchInSettings() {
        val on = preparedPrefs("sessionSize" to 12)
        settingsOver(on).setWholeChapter(true)
        val off = preparedPrefs("sessionSize" to 12)
        settingsOver(off).setWholeChapter(false)
        assertFalse(WholeChapterSuggestionViewModel(settingsOver(on)).visible.value)
        assertFalse(WholeChapterSuggestionViewModel(settingsOver(off)).visible.value)
    }

    @Test
    fun usingWholeChaptersSwitchesTheOptionOnAndClosesTheOfferForGood() = runBlocking {
        val vm = WholeChapterSuggestionViewModel(settingsOver(kept))
        vm.useWholeChapters()
        vm.visible.await { !it }
        val restarted = settingsOver(kept).prefs.value
        assertTrue(restarted.wholeChapter)
        assertFalse(restarted.suggestWholeChapter)
        assertEquals(12, restarted.sessionSize)
        assertEquals(4, restarted.newPerSession)
    }

    @Test
    fun keepingTheNumbersLeavesTheOptionOffAndTheOfferNeverReturnsAfterARestart() = runBlocking {
        val vm = WholeChapterSuggestionViewModel(settingsOver(kept))
        vm.keepNumbers()
        vm.visible.await { !it }
        val restarted = settingsOver(kept)
        assertFalse(restarted.prefs.value.wholeChapter)
        assertFalse(restarted.prefs.value.suggestWholeChapter)
        assertFalse(WholeChapterSuggestionViewModel(restarted).visible.value)
    }

    @Test
    fun anOfferThatWasNotAnsweredComesBackAtTheNextLaunch() {
        WholeChapterSuggestionViewModel(settingsOver(kept))
        assertTrue(WholeChapterSuggestionViewModel(settingsOver(kept)).visible.value)
    }
}
