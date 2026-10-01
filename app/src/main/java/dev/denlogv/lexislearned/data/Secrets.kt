package dev.denlogv.lexislearned.data

/** Storage for secrets such as API keys. */
interface Secrets {
    /**
     * Saves a secret, replacing an earlier one.
     *
     * @param name what the secret is for.
     * @param value the secret; an empty value deletes it.
     */
    fun put(name: String, value: String)

    /**
     * Reads a secret.
     *
     * @param name what the secret is for.
     * @return the secret, or null if none is saved or it cannot be decrypted.
     */
    fun get(name: String): String?
}
