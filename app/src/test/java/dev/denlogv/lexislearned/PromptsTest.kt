package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.ai.Prompts
import dev.denlogv.lexislearned.domain.CefrLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptsTest {
    @Test
    fun everyLevelHasItsOwnDefaultRules() {
        val rules = CefrLevel.entries.map { Prompts.defaultRules(it) }
        assertEquals(6, rules.toSet().size)
        CefrLevel.entries.forEach { assertTrue(Prompts.defaultRules(it).contains("{level}")) }
    }

    @Test
    fun buildFillsPlaceholdersAndKeepsTheJsonHeader() {
        val p = Prompts.build("French", "Russian", CefrLevel.C1, extra = "Focus on {source_language} law terms")
        assertFalse(p.contains("{source_language}") || p.contains("{target_language}") || p.contains("{level}"))
        assertTrue(p.contains("\"a_transcription\"")) // fixed reply format
        assertTrue(p.contains("The learner's level is C1"))
        assertTrue(p.contains("ADDITIONAL INSTRUCTIONS") && p.contains("Focus on French law terms"))
    }

    @Test
    fun customRulesReplaceTheDefaultButNotTheHeader() {
        val p = Prompts.build("English", "German", CefrLevel.B1, customRules = "ONLY {level} NOUNS for {target_language} readers")
        assertTrue(p.contains("ONLY B1 NOUNS for German readers"))
        assertFalse(p.contains("WHICH CARDS"))
        assertTrue(p.contains("Reply with JSON only"))
        assertNotEquals(p, Prompts.build("English", "German", CefrLevel.B1))
    }

    @Test
    fun blankExtraAddsNothing() {
        assertFalse(Prompts.build("English", "Russian", CefrLevel.A2, extra = "  ").contains("ADDITIONAL"))
    }
}
