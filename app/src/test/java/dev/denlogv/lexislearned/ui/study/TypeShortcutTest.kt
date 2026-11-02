package dev.denlogv.lexislearned.ui.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TypeShortcutTest {
    private val typing = TypeState(text = "cat")
    private val checked = typing.check("cat")

    @Test
    fun controlDGivesUpBeforeTheAnswerIsChecked() {
        assertEquals(TypeShortcut.GIVE_UP, typing.shortcutFor(StepKey.GiveUp))
        assertEquals(TypeShortcut.GIVE_UP, TypeState().shortcutFor(StepKey.GiveUp))
    }

    @Test
    fun controlDDoesNothingOnceTheAnswerIsChecked() {
        assertNull(checked.shortcutFor(StepKey.GiveUp))
    }

    @Test
    fun enterAndSpaceMoveOnOnceTheAnswerIsChecked() {
        assertEquals(TypeShortcut.NEXT, checked.shortcutFor(StepKey.Confirm))
        assertEquals(TypeShortcut.NEXT, checked.shortcutFor(StepKey.Space))
        assertEquals(TypeShortcut.NEXT, TypeState().giveUp().shortcutFor(StepKey.Confirm))
    }

    @Test
    fun enterAndSpaceBelongToTheFieldWhileTyping() {
        assertNull(typing.shortcutFor(StepKey.Confirm))
        assertNull(typing.shortcutFor(StepKey.Space))
    }

    @Test
    fun otherKeysDoNothing() {
        assertNull(typing.shortcutFor(StepKey.Number(1)))
        assertNull(checked.shortcutFor(StepKey.Letter(0)))
    }

    @Test
    fun shortcutsRunTheirAction() {
        val log = mutableListOf<String>()
        TypeShortcut.entries.forEach { it.perform({ log += "give up" }, { log += "next" }) }
        assertEquals(listOf("give up", "next"), log)
    }
}
