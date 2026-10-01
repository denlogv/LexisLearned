package dev.denlogv.lexislearned.ui.settings

import dev.denlogv.lexislearned.MainDispatcherRule
import dev.denlogv.lexislearned.ai.ModelInfo
import dev.denlogv.lexislearned.await
import dev.denlogv.lexislearned.data.Provider
import dev.denlogv.lexislearned.data.testSettings
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val settings = testSettings()
    private var models = listOf(ModelInfo("new-model", "New"), ModelInfo("old-model", "Old"))
    private var failure: Exception? = null
    private val requests = mutableListOf<Pair<Provider, String>>()
    private val lister = ModelLister { provider, key ->
        requests += provider to key
        failure?.let { throw it }
        models
    }

    @Test
    fun noKeyMeansNoModels() {
        val vm = SettingsViewModel(settings, lister)
        assertEquals(ModelsUi(), vm.models.value)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun savingAKeyLoadsModelsAndPicksTheNewestWhenTheSavedOneIsNotOffered() = runBlocking {
        val vm = SettingsViewModel(settings, lister)
        vm.saveKey("  sk-1 ")
        assertEquals(models, vm.models.await { it.models.isNotEmpty() }.models)
        assertEquals("new-model", vm.prefs.value.model)
        assertEquals(Provider.ANTHROPIC to "sk-1", requests.first())
    }

    @Test
    fun aSavedModelThatIsStillOfferedIsKept() = runBlocking {
        settings.setModel("old-model")
        val vm = SettingsViewModel(settings, lister)
        vm.saveKey("sk-1")
        vm.models.await { it.models.isNotEmpty() }
        assertEquals("old-model", vm.prefs.value.model)
        vm.selectModel("new-model")
        assertEquals("new-model", vm.prefs.value.model)
    }

    @Test
    fun loadingProblemsAreShownAndRefreshRetries() = runBlocking {
        failure = IOException("offline")
        val vm = SettingsViewModel(settings, lister)
        vm.saveKey("sk-1")
        assertEquals("offline", vm.models.await { it.error != null }.error)
        failure = null
        vm.refreshModels()
        assertNull(vm.models.await { it.models.isNotEmpty() }.error)
    }

    @Test
    fun removingTheKeyClearsTheModels() = runBlocking {
        val vm = SettingsViewModel(settings, lister)
        vm.saveKey("sk-1")
        vm.models.await { it.models.isNotEmpty() }
        vm.removeKey()
        assertEquals(ModelsUi(), vm.models.await { it.models.isEmpty() })
        assertEquals(false, vm.prefs.value.hasApiKey)
    }

    @Test
    fun switchingProviderReloadsForTheNewKey() = runBlocking {
        settings.setApiKey(Provider.OPENAI, "sk-openai")
        val vm = SettingsViewModel(settings, lister)
        settings.setProvider(Provider.OPENAI)
        vm.models.await { it.models.isNotEmpty() }
        assertEquals(Provider.OPENAI to "sk-openai", requests.last())
    }
}
