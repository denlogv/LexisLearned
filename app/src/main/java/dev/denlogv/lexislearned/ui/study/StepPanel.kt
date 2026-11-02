package dev.denlogv.lexislearned.ui.study

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How a study step arranges its content and its buttons. */
enum class StepLayout {
    /** The buttons sit below the content. */
    STACKED,

    /** The buttons sit beside the content, for a panel that is wider than it is tall, such as a phone held sideways. */
    SIDE_BY_SIDE,
}

/**
 * The area a study step is drawn in, and how the step arranges itself in it.
 *
 * @property width the panel's width.
 * @property height the panel's height.
 * @property layout how the step arranges its content and buttons in this panel.
 */
data class StepPanel(val width: Dp, val height: Dp, val layout: StepLayout)

/** The widest a panel gets; on a wider window (Samsung DeX, a tablet) the panel stays this wide and is centered. */
private val MAX_PANEL_WIDTH = 720.dp

/** The tallest a panel gets on a window that is wider than [MAX_PANEL_WIDTH]; on a phone the panel always uses the full height. */
private val MAX_PANEL_HEIGHT = 640.dp

/**
 * Works out how big the study panel is and how a step arranges itself in it. A phone keeps using the whole screen. On a wider
 * window the panel is capped in both directions, so a card does not stretch across a desktop-sized display.
 *
 * The layout follows the panel's own shape rather than the device's orientation, because a window on a desktop-like display
 * can be landscape and still be tall enough to stack the buttons below the card. A panel as tall as [MAX_PANEL_HEIGHT] is such
 * a window: the capped panel is wider than tall, yet roomy enough to stack, so it is never put side by side.
 *
 * @param availableWidth the width the study screen has, below its top bar.
 * @param availableHeight the height the study screen has, below its top bar and progress bar.
 * @return the panel's size and layout.
 */
fun stepPanelFor(availableWidth: Dp, availableHeight: Dp): StepPanel {
    val wide = availableWidth > MAX_PANEL_WIDTH
    val width = if (wide) MAX_PANEL_WIDTH else availableWidth
    val height = if (wide) minOf(availableHeight, MAX_PANEL_HEIGHT) else availableHeight
    val sideBySide = width > height && height < MAX_PANEL_HEIGHT
    return StepPanel(width, height, if (sideBySide) StepLayout.SIDE_BY_SIDE else StepLayout.STACKED)
}
