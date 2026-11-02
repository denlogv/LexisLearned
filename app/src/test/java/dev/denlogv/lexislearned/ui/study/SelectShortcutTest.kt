package dev.denlogv.lexislearned.ui.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectShortcutTest {
    private val options = listOf("a", "b", "c", "d")

    private fun shortcut(key: StepKey, picked: String? = null) = selectShortcutFor(key, options, picked)

    @Test
    fun numberKeysPickTheAnswerAtThatPosition() {
        assertEquals(SelectShortcut.Pick("a"), shortcut(StepKey.Number(1)))
        assertEquals(SelectShortcut.Pick("d"), shortcut(StepKey.Number(4)))
    }

    @Test
    fun aNumberWithoutAnAnswerDoesNothing() {
        assertNull(shortcut(StepKey.Number(5)))
        assertNull(shortcut(StepKey.Number(10)))
    }

    @Test
    fun theAnswerCannotBeChangedOncePicked() {
        assertNull(shortcut(StepKey.Number(2), picked = "a"))
    }

    @Test
    fun enterAndSpaceMoveOnOnlyAfterPicking() {
        assertNull(shortcut(StepKey.Confirm))
        assertNull(shortcut(StepKey.Space))
        assertEquals(SelectShortcut.Next, shortcut(StepKey.Confirm, picked = "b"))
        assertEquals(SelectShortcut.Next, shortcut(StepKey.Space, picked = "b"))
    }

    @Test
    fun otherKeysDoNothing() {
        assertNull(shortcut(StepKey.Letter(0)))
        assertNull(shortcut(StepKey.GiveUp, picked = "a"))
    }

    @Test
    fun shortcutsRunTheirAction() {
        val log = mutableListOf<String>()
        SelectShortcut.Pick("b").perform({ log += "pick $it" }, { log += "next" })
        SelectShortcut.Next.perform({ log += "pick $it" }, { log += "next" })
        assertEquals(listOf("pick b", "next"), log)
    }
}
