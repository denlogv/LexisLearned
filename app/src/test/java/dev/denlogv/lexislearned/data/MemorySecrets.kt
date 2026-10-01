package dev.denlogv.lexislearned.data

/** Keeps secrets in a map instead of the Android Keystore. */
class MemorySecrets : Secrets {
    private val map = HashMap<String, String>()

    override fun put(name: String, value: String) {
        if (value.isEmpty()) map.remove(name) else map[name] = value
    }

    override fun get(name: String): String? = map[name]
}
