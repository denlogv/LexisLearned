package dev.denlogv.lexislearned.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType

/**
 * A labelled field for a whole number. Every valid number is saved as soon as it is typed; text that is not usable is flagged
 * and not saved, and when the field loses focus it shows the number in use again.
 *
 * @param label what the number is.
 * @param value the number in use.
 * @param range the allowed numbers.
 * @param onChange called with each valid number typed.
 */
@Composable
fun CountSetting(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    var draft by remember(value) { mutableStateOf(value.toString()) }
    val rejected = parseCount(draft, range) == CountEntry.Rejected
    OutlinedTextField(
        draft,
        { text ->
            draft = text
            (parseCount(text, range) as? CountEntry.Accepted)?.let { onChange(it.value) }
        },
        Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) draft = value.toString() },
        label = { Text(label) },
        isError = rejected,
        supportingText = { Text(countSupportText(range, rejected)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
