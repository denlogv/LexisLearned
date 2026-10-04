package dev.denlogv.lexislearned.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PauseRequestTest {
    @Test
    fun eachPauseButtonHasAnActionOfItsOwnThatReadsBackAsTheSameRequest() {
        assertNotEquals(PauseRequest.AFTER_SECTION.action, PauseRequest.NOW.action)
        assertEquals(PauseRequest.AFTER_SECTION, PauseRequest.fromAction(PauseRequest.AFTER_SECTION.action))
        assertEquals(PauseRequest.NOW, PauseRequest.fromAction(PauseRequest.NOW.action))
    }

    @Test
    fun anythingElseIsNoRequest() {
        assertEquals(PauseRequest.NONE, PauseRequest.fromAction(null))
        assertEquals(PauseRequest.NONE, PauseRequest.fromAction("android.intent.action.MAIN"))
    }
}
