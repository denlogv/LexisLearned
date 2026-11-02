package dev.denlogv.lexislearned.ui.study

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** How the step that is being drawn arranges itself; set by [StepPanelArea]. */
private val LocalStepLayout = compositionLocalOf { StepLayout.STACKED }

/**
 * Whether the current step puts its buttons beside its content instead of below it.
 *
 * @return true in a panel that is wider than it is tall.
 */
@Composable
fun isSideBySide(): Boolean = LocalStepLayout.current == StepLayout.SIDE_BY_SIDE

/**
 * Draws a study step in a panel that is centered in the space it is given and capped in size, see [stepPanelFor].
 *
 * @param modifier layout modifier of the whole area.
 * @param content what goes in the panel; steps read [isSideBySide] to arrange themselves.
 */
@Composable
fun StepPanelArea(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val panel = stepPanelFor(maxWidth, maxHeight)
        CompositionLocalProvider(LocalStepLayout provides panel.layout) {
            Column(Modifier.size(panel.width, panel.height), content = content)
        }
    }
}
