package dev.denlogv.lexislearned.ai

import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Provider

/** What differs between the AI providers when a deck is generated: which client talks to them and what must be set up first. */
internal object Providers {
    /**
     * Says what the user still has to set up in Settings.
     *
     * @param prefs the current settings.
     * @return a message for the failure screen.
     */
    fun missingSetting(prefs: Prefs): String = when {
        !prefs.hasApiKey -> "Add your ${prefs.provider.label} API key in Settings first."
        prefs.endpoint == null -> "Enter a valid http:// or https:// server address in Settings first."
        else -> "Choose a model in Settings first."
    }

    /**
     * Creates the client for the real provider APIs.
     *
     * @param prefs the settings naming the provider, the model and, for a custom server, its address.
     * @param key the user's API key.
     * @return the client.
     * @throws LlmException if a custom server is selected but its address is not valid.
     */
    fun defaultClient(prefs: Prefs, key: String): LlmClient = when (prefs.provider) {
        Provider.ANTHROPIC -> AnthropicClient(key, prefs.model)
        Provider.OPENAI -> OpenAiClient(key, prefs.model)
        Provider.OPENAI_COMPATIBLE ->
            OpenAiClient(key, prefs.model, prefs.endpoint ?: throw LlmException("The server address is not valid"))
    }
}
