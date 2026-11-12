package dev.denlogv.lexislearned.data

import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsTest {
    private val settings = testSettings()

    @Test
    fun defaultsAreSensible() {
        val p = settings.prefs.value
        assertEquals(StudyMode.entries.toSet(), p.modes)
        assertEquals(1, p.sessionsToComplete)
        assertEquals(6, p.pairSize)
        assertTrue(p.showTimeLeft)
        assertEquals(1, p.timeLeftAfter)
        assertTrue(p.wholeChapter)
        assertFalse(p.hasApiKey)
        assertEquals(Provider.ANTHROPIC.defaultModel, p.model)
    }

    @Test
    fun studyOptionsAreSavedAndClamped() {
        settings.setDirection(Direction.REVERSE)
        settings.setSessionSize(12)
        settings.setNewPerSession(3)
        settings.setRounds(99)
        settings.setSessionsToComplete(0)
        settings.setSpaceSessions(false)
        val p = settings.prefs.value
        assertEquals(Direction.REVERSE, p.direction)
        assertEquals(12, p.sessionSize)
        assertEquals(3, p.newPerSession)
        assertEquals(5, p.rounds)
        assertEquals(1, p.sessionsToComplete)
        assertFalse(p.spaceSessions)
    }

    @Test
    fun pairSizeIsSavedAndClamped() {
        settings.setPairSize(9)
        assertEquals(9, settings.prefs.value.pairSize)
        settings.setPairSize(1)
        assertEquals(3, settings.prefs.value.pairSize)
        settings.setPairSize(99)
        assertEquals(12, settings.prefs.value.pairSize)
    }

    @Test
    fun timeLeftOptionsAreSavedAndClamped() {
        settings.setShowTimeLeft(false)
        settings.setTimeLeftAfter(4)
        assertFalse(settings.prefs.value.showTimeLeft)
        assertEquals(4, settings.prefs.value.timeLeftAfter)
        settings.setTimeLeftAfter(0)
        assertEquals(1, settings.prefs.value.timeLeftAfter)
        settings.setTimeLeftAfter(99)
        assertEquals(10, settings.prefs.value.timeLeftAfter)
    }

    @Test
    fun atLeastOneModeStaysSelected() {
        StudyMode.entries.drop(1).forEach(settings::toggleMode)
        assertEquals(setOf(StudyMode.LEARN), settings.prefs.value.modes)
        settings.toggleMode(StudyMode.LEARN)
        assertEquals(setOf(StudyMode.LEARN), settings.prefs.value.modes)
        settings.toggleMode(StudyMode.TYPE)
        assertEquals(setOf(StudyMode.LEARN, StudyMode.TYPE), settings.prefs.value.modes)
    }

    @Test
    fun generationOptionsArePerProvider() {
        settings.setModel("claude-x")
        settings.setProvider(Provider.OPENAI)
        assertEquals(Provider.OPENAI.defaultModel, settings.prefs.value.model)
        settings.setModel(" gpt-y ")
        settings.setProvider(Provider.ANTHROPIC)
        assertEquals("claude-x", settings.prefs.value.model)
        settings.setLevel(CefrLevel.C1)
        settings.setTargetLang(" DE ")
        settings.setExtraInstructions("be brief")
        val p = settings.prefs.value
        assertEquals(CefrLevel.C1, p.level)
        assertEquals("de", p.targetLang)
        assertEquals("be brief", p.extraInstructions)
    }

    @Test
    fun customRulesCanBeSetAndRemoved() {
        settings.setCustomRules(CefrLevel.A1, "my rules")
        assertEquals("my rules", settings.prefs.value.customRules[CefrLevel.A1])
        settings.setCustomRules(CefrLevel.A1, " ")
        assertTrue(settings.prefs.value.customRules.isEmpty())
    }

    @Test
    fun apiKeysAreKeptInSecretsPerProvider() {
        assertNull(settings.apiKey())
        settings.setApiKey(Provider.ANTHROPIC, "  sk-1 ")
        assertEquals("sk-1", settings.apiKey())
        assertTrue(settings.prefs.value.hasApiKey)
        settings.setProvider(Provider.OPENAI)
        assertFalse(settings.prefs.value.hasApiKey)
        assertEquals("sk-1", settings.apiKey(Provider.ANTHROPIC))
    }

    @Test
    fun theCustomServerAddressIsSavedTrimmedAndValidatedOnUse() {
        settings.setProvider(Provider.OPENAI_COMPATIBLE)
        assertNull(settings.prefs.value.endpoint)
        settings.setBaseUrl("  http://localhost:11434/v1/ ")
        assertEquals("http://localhost:11434/v1/", settings.prefs.value.baseUrl)
        assertEquals("http://localhost:11434/v1", settings.prefs.value.endpoint)
        settings.setBaseUrl("not a url")
        assertNull(settings.prefs.value.endpoint)
    }

    @Test
    fun aCustomServerIsReadyOnlyWithKeyAddressAndModel() {
        settings.setProvider(Provider.OPENAI_COMPATIBLE)
        settings.setApiKey(Provider.OPENAI_COMPATIBLE, "k")
        assertFalse(settings.prefs.value.ready)
        settings.setBaseUrl("https://api.example.com/v1")
        assertFalse(settings.prefs.value.ready)
        settings.setModel("m")
        assertTrue(settings.prefs.value.ready)
        settings.setProvider(Provider.ANTHROPIC)
        assertFalse(settings.prefs.value.ready)
        settings.setApiKey(Provider.ANTHROPIC, "k")
        assertTrue(settings.prefs.value.ready)
    }

    @Test
    fun sessionNumbersAreClampedWhenSet() {
        settings.setSessionSize(0)
        settings.setNewPerSession(-4)
        assertEquals(SESSION_SIZE_RANGE.first, settings.prefs.value.sessionSize)
        assertEquals(NEW_PER_SESSION_RANGE.first, settings.prefs.value.newPerSession)
        settings.setSessionSize(5_000)
        settings.setNewPerSession(Int.MAX_VALUE)
        assertEquals(MAX_SESSION_CARDS, settings.prefs.value.sessionSize)
        assertEquals(MAX_SESSION_CARDS, settings.prefs.value.newPerSession)
        settings.setSessionSize(250)
        settings.setNewPerSession(0)
        assertEquals(250, settings.prefs.value.sessionSize)
        assertEquals(0, settings.prefs.value.newPerSession)
    }

    @Test
    fun sessionNumbersStoredOutOfRangeAreClampedWhenLoaded() {
        val p = settingsOver(preparedPrefs("sessionSize" to 0, "newPerSession" to 123_456)).prefs.value
        assertEquals(1, p.sessionSize)
        assertEquals(MAX_SESSION_CARDS, p.newPerSession)
    }

    @Test
    fun sessionNumbersChosenBeforeAreKept() {
        val p = settingsOver(preparedPrefs("sessionSize" to 25, "newPerSession" to 8)).prefs.value
        assertEquals(25, p.sessionSize)
        assertEquals(8, p.newPerSession)
    }
}
