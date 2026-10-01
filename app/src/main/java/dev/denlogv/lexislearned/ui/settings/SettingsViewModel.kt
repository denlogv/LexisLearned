package dev.denlogv.lexislearned.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
     * @param provider the provider to ask.
     * @param apiKey the user's key for that provider.
     * @return the models, newest first.
     */
    suspend fun list(provider: Provider, apiKey: String): List<ModelInfo>
}

/** The [ModelLister] that talks to the real provider APIs. */
object RemoteModelLister : ModelLister {
    /**
     * Lists the models through the provider's API.
     *
     * @param provider the provider to ask.
     * @param apiKey the user's key for that provider.
     * @return the models, newest first.
     */
    override suspend fun list(provider: Provider, apiKey: String): List<ModelInfo> = when (provider) {
        Provider.ANTHROPIC -> ModelCatalog().anthropic(apiKey)
        Provider.OPENAI -> ModelCatalog().openAi(apiKey)
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
        val provider = prefs.value.provider
        val key = settings.apiKey(provider)
        if (key.isNullOrBlank()) {
            _models.value = ModelsUi()
            return
        }
        loadJob = viewModelScope.launch { load(provider, key) }
    }

    /**
     * Requests the models and keeps the selection valid: if the saved model is not offered, the newest one is chosen.
     *
     * @param provider the provider to ask.
     * @param key the API key.
     */
    @Suppress("TooGenericExceptionCaught") // Network, parsing and provider errors all end up as a message next to the picker.
    private suspend fun load(provider: Provider, key: String) {
        _models.value = ModelsUi(loading = true)
        _models.value = try {
            val list = lister.list(provider, key)
            if (list.isNotEmpty() && list.none { it.id == prefs.value.model }) settings.setModel(list.first().id)
            ModelsUi(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ModelsUi(error = e.message ?: "Could not load models")
        }
    }
}
