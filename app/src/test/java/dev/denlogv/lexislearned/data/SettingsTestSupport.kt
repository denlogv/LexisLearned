package dev.denlogv.lexislearned.data

import android.content.Context
import java.util.UUID

/**
 * Settings on a fresh preferences file with in-memory secrets.
 *
 * @param secrets where API keys go.
 */
fun testSettings(secrets: Secrets = MemorySecrets()): Settings =
    Settings(testContext().getSharedPreferences("test-" + UUID.randomUUID(), Context.MODE_PRIVATE), secrets)
