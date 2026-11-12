package dev.denlogv.lexislearned.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Asks whether to carry on with the session that was left unfinished or to start a new one.
 *
 * @param status what is left of the unfinished session, for example "12 steps left · 2/5 words done".
 * @param onContinue called when the learner wants to carry on with the unfinished session.
 * @param onStartNew called when the learner wants a new session; the unfinished one is thrown away.
 */
@Composable
fun ResumePrompt(status: String, onContinue: () -> Unit, onStartNew: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Unfinished session", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text("You stopped a session here before the end.", Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        Text(status, Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onContinue, Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Continue") }
        OutlinedButton(onStartNew, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Start a new session") }
    }
}
