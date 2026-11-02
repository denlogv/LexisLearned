package dev.denlogv.lexislearned.ui.study

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class StepPanelTest {
    @Test
    fun phoneHeldUprightUsesWholeScreenAndStacksButtons() {
        assertEquals(StepPanel(360.dp, 640.dp, StepLayout.STACKED), stepPanelFor(360.dp, 640.dp))
    }

    @Test
    fun phoneHeldSidewaysKeepsHeightAndPutsButtonsBesideCard() {
        assertEquals(StepPanel(720.dp, 290.dp, StepLayout.SIDE_BY_SIDE), stepPanelFor(800.dp, 290.dp))
    }

    @Test
    fun desktopWindowGetsCappedPanelWithButtonsBelowCard() {
        assertEquals(StepPanel(720.dp, 640.dp, StepLayout.STACKED), stepPanelFor(1920.dp, 1000.dp))
    }

    @Test
    fun wideShortWindowKeepsHeightAndPutsButtonsBesideCard() {
        assertEquals(StepPanel(720.dp, 400.dp, StepLayout.SIDE_BY_SIDE), stepPanelFor(900.dp, 400.dp))
    }

    @Test
    fun tabletHeldUprightIsCappedInBothDirections() {
        assertEquals(StepPanel(720.dp, 640.dp, StepLayout.STACKED), stepPanelFor(800.dp, 1200.dp))
    }

    @Test
    fun windowExactlyAsWideAsCapIsNotCapped() {
        assertEquals(StepPanel(720.dp, 1000.dp, StepLayout.STACKED), stepPanelFor(720.dp, 1000.dp))
    }

    @Test
    fun squarePanelStacksButtons() {
        assertEquals(StepLayout.STACKED, stepPanelFor(500.dp, 500.dp).layout)
    }
}
