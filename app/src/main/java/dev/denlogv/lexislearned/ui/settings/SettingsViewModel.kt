package dev.denlogv.lexislearned.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.denlogv.lexislearned.ai.LlmException
import dev.denlogv.lexislearned.ai.ModelCatalog
import dev.denlogv.lexislearned.ai.ModelInfo
import dev.denlogv.lexislearned.data.Prefs
import dev.denlogv.lexislearned.data.Provider
import dev.denlogv.lexislearned.data.Settings
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Asks a provider which models an API key may use. */
fun interface ModelLister {
    /**
     * Lists the models.
     *
     * @param prefs the settings naming the provider and, for a custom server, its address.
     * @param apiKey the user's key for that provider.
     * @return the models, newest first.
     * @throws dev.denlogv.lexislearned.ai.LlmException if the request fails or a custom server has no valid address.
     */
    suspend fun list(prefs: Prefs, apiKey: String): List<ModelInfo>
}

/** The [ModelLister] that talks to the real provider APIs. */
object RemoteModelLister : ModelLister {
    /**
     * Lists the models through the provider's API.
     *
     * @param prefs the settings naming the provider and, for a custom server, its address.
     * @param apiKey the user's key for that provider.
     * @return the models, newest first.
     * @throws LlmException if the request fails or a custom server has no valid address.
     */
    override suspend fun list(prefs: Prefs, apiKey: String): List<ModelInfo> = when (prefs.provider) {
        Provider.ANTHROPIC -> ModelCatalog().anthropic(apiKey)
        Provider.OPENAI -> ModelCatalog().openAi(apiKey)
        Provider.OPENAI_COMPATIBLE ->
            ModelCatalog().openAiCompatible(apiKey, prefs.endpoint ?: throw LlmException("Enter a valid http:// or https:// address"))
    }
}

/**
 * What the model picker shows.
 *
 * @property models the models the key may use; empty until loaded.
 * @property loading whether a request is running.
 * @property error why the last request failed, if it did.
 */
data class ModelsUi(val models: List<ModelInfo> = emptyList(), val loading: Boolean = false, val error: String? = null)

/**
 * The state and actions of the settings screen.
 *
 * Simple options are changed through [settings] directly; this class adds what needs logic: saving and removing the API key
 * and loading the model list whenever the provider or the presence of a key changes.
 *
 * @property settings the user's settings; the screen calls its setters for plain options.
 * @param lister loads the model list.
 */
class SettingsViewModel(val settings: Settings, private val lister: ModelLister = RemoteModelLister) : ViewModel() {
    private val _models = MutableStateFlow(ModelsUi())
    private var loadJob: Job? = null

    /** The current settings. */
    val prefs: StateFlow<Prefs> = settings.prefs

    /** The model list state. */
    val models: StateFlow<ModelsUi> = _models

    init {
        viewModelScope.launch {
            prefs.map { it.provider to it.hasApiKey }.distinctUntilChanged().collect { refreshModels() }
        }
    }

    /**
     * Saves the API key for the current provider.
     *
     * @param key the key as typed; blank input removes the key.
     */
    fun saveKey(key: String) = settings.setApiKey(prefs.value.provider, key.trim())

    /** Removes the API key of the current provider. */
    fun removeKey() = saveKey("")

    /**
     * Picks the model used for deck generation.
     *
     * @param id the model id; it should come from [models].
     */
    fun selectModel(id: String) = settings.setModel(id)

    /** Loads the model list again, or clears it if no key is saved. */
    fun refreshModels() {
        loadJob?.cancel()
        val key = settings.apiKey(prefs.value.provider)
        if (key.isNullOrBlank()) {
            _models.value = ModelsUi()
            return
        }
        loadJob = viewModelScope.launch { load(prefs.value, key) }
    }

    /**
     * Requests the models and keeps the selection valid: if the saved model is not offered, the newest one is chosen. A custom
     * server may serve models it does not list, so there the choice is only filled in when empty.
     *
     * @param current the settings at the time of the request.
     * @param key the API key.
     */
    @Suppress("TooGenericExceptionCaught") // Network, parsing and provider errors all end up as a message next to the picker.
    private suspend fun load(current: Prefs, key: String) {
        _models.value = ModelsUi(loading = true)
        _models.value = try {
            val list = lister.list(current, key)
            if (list.isNotEmpty() && shouldReplaceModel(current, list)) settings.setModel(list.first().id)
            ModelsUi(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ModelsUi(error = e.message ?: "Could not load models")
        }
    }

    /**
     * Whether the saved model should be swapped for the newest listed one.
     *
     * @param current the settings.
     * @param list the models the provider offers.
     * @return true if the saved model is not offered, except on a custom server where only an empty choice is replaced.
     */
    private fun shouldReplaceModel(current: Prefs, list: List<ModelInfo>): Boolean = when (current.provider) {
        Provider.OPENAI_COMPATIBLE -> current.model.isBlank()
        else -> list.none { it.id == prefs.value.model }
    }
}
