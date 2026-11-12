package dev.denlogv.lexislearned.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Settings on a fresh preferences file with in-memory secrets.
 *
 * @param secrets where API keys go.
 */
fun testSettings(secrets: Secrets = MemorySecrets()): Settings =
    Settings(testContext().getSharedPreferences("test-" + UUID.randomUUID(), Context.MODE_PRIVATE), secrets)

/**
 * A fresh preferences file holding the given values, as an older version of the app might have left it.
 *
 * @param entries key and value pairs; values are Int or Boolean.
 * @return the preferences.
 */
fun preparedPrefs(vararg entries: Pair<String, Any>): SharedPreferences {
    val sp = testContext().getSharedPreferences("prepared-" + UUID.randomUUID(), Context.MODE_PRIVATE)
    sp.edit().apply {
        entries.forEach { (key, value) ->
            when (value) {
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                else -> error("unsupported test value for $key")
            }
        }
    }.apply()
    return sp
}

/**
 * Settings that open an existing preferences file, as at the next launch of the app.
 *
 * @param sp the preferences.
 * @return the settings, with in-memory secrets.
 */
fun settingsOver(sp: SharedPreferences): Settings = Settings(sp, MemorySecrets())
