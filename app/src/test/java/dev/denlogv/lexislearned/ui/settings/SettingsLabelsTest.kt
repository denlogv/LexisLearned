package dev.denlogv.lexislearned.ui.settings

import dev.denlogv.lexislearned.ai.ModelInfo
import dev.denlogv.lexislearned.data.Prefs
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
}
