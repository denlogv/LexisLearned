package dev.denlogv.lexislearned.service

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ServiceKeepAliveTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun holdingStartsTheGenerationService() {
        ServiceKeepAlive(app).hold()
        val started = shadowOf(app).nextStartedService
        assertNotNull(started)
        assertEquals(GenerationService::class.java.name, started.component!!.className)
    }

    @Test
    fun aServiceTheSystemWillNotStartDoesNotStopTheGeneration() {
        ServiceKeepAlive(refusing(IllegalStateException("app is in the background"))).hold()
        ServiceKeepAlive(refusing(SecurityException("no permission"))).hold()
    }

    private fun refusing(error: RuntimeException) = object : ContextWrapper(app) {
        override fun startForegroundService(service: Intent?) = throw error

        override fun getApplicationContext(): Context = this
    }
}
