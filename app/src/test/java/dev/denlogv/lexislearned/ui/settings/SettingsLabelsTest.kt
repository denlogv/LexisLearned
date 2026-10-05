package dev.denlogv.lexislearned.ui.settings

import dev.denlogv.lexislearned.ai.ModelInfo
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Provider
import dev.denlogv.lexislearned.domain.CefrLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLabelsTest {
    @Test
    fun sessionSummaryMultipliesWordsModesAndRounds() {
        val text = sessionSummary(Prefs(sessionSize = 10, rounds = 2))
        assertEquals(true, text.startsWith("A full session is about 100 steps (10 words × 5 modes × 2 rounds)."))
    }

    @Test
    fun modelButtonExplainsWhyThereIsNoModel() {
        val list = listOf(ModelInfo("m1", "Model One"))
        assertEquals("Model One", modelButtonLabel(ModelsUi(list), "m1"))
        assertEquals("other", modelButtonLabel(ModelsUi(list), "other"))
        assertEquals("Loading models…", modelButtonLabel(ModelsUi(loading = true), "m1"))
        assertEquals("No models loaded", modelButtonLabel(ModelsUi(), "m1"))
    }

    @Test
    fun menuAndFieldLabels() {
        assertEquals("m1", modelMenuLabel(ModelInfo("m1", "m1")))
        assertEquals("One (m1)", modelMenuLabel(ModelInfo("m1", "One")))
        assertEquals("Card rules for B1 (default)", rulesFieldLabel("B1", false))
        assertEquals("Card rules for B1 (customised)", rulesFieldLabel("B1", true))
    }

    @Test
    fun studySummaryNamesTheSessionChoices() {
        assertEquals(
            "10 cards per session · 5 new · 1 round · pairs of 6",
            studySummary(Prefs(sessionSize = 10, newPerSession = 5, rounds = 1, pairSize = 6)),
        )
        assertEquals(
            "1 card per session · 0 new · 3 rounds · pairs of 12",
            studySummary(Prefs(sessionSize = 1, newPerSession = 0, rounds = 3, pairSize = 12)),
        )
    }

    @Test
    fun generationSummaryNamesProviderModelAndKey() {
        val saved = Prefs(provider = Provider.ANTHROPIC, model = "claude-x", hasApiKey = true)
        assertEquals("Claude · claude-x · key saved", generationSummary(saved))
        assertEquals("OAI-compatible · no key yet", generationSummary(Prefs(provider = Provider.OPENAI_COMPATIBLE, model = " ")))
    }

    @Test
    fun promptSummaryTellsWhatWasEdited() {
        assertEquals("B1 · default rules", promptSummary(Prefs(level = CefrLevel.B1)))
        val edited = Prefs(level = CefrLevel.C1, customRules = mapOf(CefrLevel.C1 to "my rules"), extraInstructions = "legal")
        assertEquals("C1 · customised rules · extra instructions", promptSummary(edited))
        assertEquals("C1 · default rules", promptSummary(Prefs(level = CefrLevel.C1, customRules = mapOf(CefrLevel.B1 to "x"))))
    }
}
