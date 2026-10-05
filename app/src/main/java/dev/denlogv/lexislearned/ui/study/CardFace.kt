package dev.denlogv.lexislearned.ui.study

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Whether the device is held sideways, where the study steps put their controls beside the content instead of below it.
 *
 * @return true in landscape orientation.
 */
@Composable
fun isLandscape(): Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * A word with its pronunciation line underneath.
 *
 * @param word the word or phrase.
 * @param hint the pronunciation, or null if there is none.
 * @param reserveHint whether a missing pronunciation still leaves an empty line, so the word sits at the same place as on a
 *   card side that has one.
 */
@Composable
fun WordBlock(word: String, hint: String?, reserveHint: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(word, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        if (hint != null || reserveHint) {
            Text(
                hint?.let { "[$it]" } ?: " ",
                Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Where the first line of the word starts, as a fraction of the card's height from its top. */
private const val WORD_TOP_FRACTION = 0.35f

/** The same in landscape, where the card is short and the example and hint would otherwise not fit below the word. */
private const val LANDSCAPE_WORD_TOP_FRACTION = 0.1f

/**
 * One side of a flash card. The first line of the word starts at the same height on every side, a little above the vertical
 * middle (near the top in landscape, where the card is short), so the front and the back line up even when a word or
 * translation wraps onto more lines (the extra lines grow downwards). The word takes as much room as the taller of the two
 * sides' words and the pronunciation line is always reserved, so what follows (the example and the hint) also starts at the
 * same height on both sides. A side that is taller than the card scrolls.
 *
 * @param word the word or phrase of this side.
 * @param hint the pronunciation of this side, or null; an empty line is kept for it.
 * @param otherWord the word or phrase on the other side of the card, whose height is reserved but which is not shown.
 * @param below the content under the word: example and hint to tap.
 */
@Composable
fun CardFace(word: String, hint: String?, otherWord: String, below: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val top = maxHeight * if (isLandscape()) LANDSCAPE_WORD_TOP_FRACTION else WORD_TOP_FRACTION
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(top))
            Box(contentAlignment = Alignment.TopCenter) {
                WordBlock(word, hint, reserveHint = true)
                // Invisible and silent: only takes up the other side's height, so both sides lay out what follows alike.
                Box(Modifier.alpha(0f).clearAndSetSemantics {}) { WordBlock(otherWord, null, reserveHint = true) }
            }
            below()
        }
    }
}
