package dev.denlogv.lexislearned.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Provider

/**
 * The settings for creating decks from EPUBs, in three groups: the provider with its key, the model and the user's language.
 *
 * @param prefs the current settings.
 * @param vm the settings view model.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerationSettings(prefs: Prefs, vm: SettingsViewModel) {
    Hint("Chapters are sent to the AI provider you choose, using your own API key. The key is stored encrypted on this device only.")
    SettingsGroup("Provider") {
        ProviderAndKey(prefs, vm)
    }
    SettingsGroup("Model") {
        ModelPicker(prefs, vm)
        if (prefs.provider == Provider.OPENAI_COMPATIBLE) ModelIdField(prefs, vm)
    }
    SettingsGroup("Your language") {
        OutlinedTextField(
            prefs.targetLang,
            vm.settings::setTargetLang,
            Modifier.fillMaxWidth(),
            label = { Text("ISO code, e.g. ru, de, es") },
            singleLine = true,
        )
    }
}

/**
 * The provider chips, the server address of a custom server, and the API key field with its buttons.
 *
 * @param prefs the current settings.
 * @param vm the settings view model.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderAndKey(prefs: Prefs, vm: SettingsViewModel) {
    var key by remember(prefs.provider) { mutableStateOf("") }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Provider.entries.forEach { p -> FilterChip(prefs.provider == p, { vm.settings.setProvider(p) }, { Text(p.chip) }) }
    }
    if (prefs.provider == Provider.OPENAI_COMPATIBLE) ServerAddress(prefs, vm)
    OutlinedTextField(
        key,
        { key = it },
        Modifier.fillMaxWidth(),
        label = { Text(if (prefs.hasApiKey) "API key (saved)" else "API key") },
        supportingText = if (prefs.hasApiKey) ({ Text("Enter a new key to replace it") }) else null,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({
            vm.saveKey(key)
            key = ""
        }, enabled = key.isNotBlank()) { Text("Save key") }
        if (prefs.hasApiKey) OutlinedButton(vm::removeKey) { Text("Remove key") }
    }
}

/**
 * The model drop-down; the user can only pick from the list the provider returns.
 *
 * @param prefs the current settings.
 * @param vm the settings view model.
 */
@Composable
private fun ModelPicker(prefs: Prefs, vm: SettingsViewModel) {
    val ui by vm.models.collectAsState()
    var menu by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            OutlinedButton({ menu = true }, Modifier.fillMaxWidth(), enabled = ui.models.isNotEmpty()) {
                Text(modelButtonLabel(ui, prefs.model), maxLines = 1)
            }
            DropdownMenu(menu, { menu = false }) {
                ui.models.forEach { m ->
                    DropdownMenuItem({ Text(modelMenuLabel(m)) }, {
                        vm.selectModel(m.id)
                        menu = false
                    })
                }
            }
        }
        if (prefs.hasApiKey) OutlinedButton(vm::refreshModels, enabled = !ui.loading) { Text(if (ui.loading) "…" else "Refresh") }
    }
    ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    if (!prefs.hasApiKey) Hint("Save an API key to load the models available to it.")
}

/**
 * The address field of a custom OpenAI-compatible server, with a note on what is accepted.
 *
 * @param prefs the current settings.
 * @param vm the settings view model.
 */
@Composable
private fun ServerAddress(prefs: Prefs, vm: SettingsViewModel) {
    OutlinedTextField(
        prefs.baseUrl,
        vm.settings::setBaseUrl,
        Modifier.fillMaxWidth(),
        label = { Text("Server address, e.g. https://api.example.com/v1") },
        isError = prefs.baseUrl.isNotBlank() && prefs.endpoint == null,
        singleLine = true,
    )
    Hint(
        "Any server that speaks the OpenAI chat API: OpenRouter, Groq, Ollama, LM Studio and others. Include the version " +
            "path (usually /v1). Plain http:// is allowed for servers on your own network, but then your key and the book " +
            "text travel unencrypted. If the server needs no key, save any placeholder.",
    )
}

/**
 * A text field for the model id, because a custom server may not list its models or may serve more than it lists.
 *
 * @param prefs the current settings.
 * @param vm the settings view model.
 */
@Composable
private fun ModelIdField(prefs: Prefs, vm: SettingsViewModel) {
    OutlinedTextField(
        prefs.model,
        vm::selectModel,
        Modifier.fillMaxWidth(),
        label = { Text("Model id") },
        singleLine = true,
    )
}
