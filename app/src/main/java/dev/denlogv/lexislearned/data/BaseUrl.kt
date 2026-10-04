package dev.denlogv.lexislearned.data

import java.net.URI
import java.net.URISyntaxException

private val SCHEMES = setOf("http", "https")
private const val CHAT_PATH = "/chat/completions"

/**
 * Turns what the user typed as the address of an OpenAI-compatible server into the base URL the client appends paths to.
 *
 * Surrounding spaces and trailing slashes are removed, and so is a pasted `/chat/completions` endpoint, because the client
 * adds that part itself. The path before it (usually `/v1`) is kept as typed.
 *
 * @param input the address as typed.
 * @return the base URL, or null if it is not an `http` or `https` address with a host, or carries a query or fragment.
 */
fun normalizeBaseUrl(input: String): String? {
    val text = input.trim().trimEnd('/').removeSuffix(CHAT_PATH).trimEnd('/')
    val uri = try {
        URI(text)
    } catch (_: URISyntaxException) {
        return null
    }
    val valid = uri.scheme?.lowercase() in SCHEMES && !uri.host.isNullOrEmpty() && uri.query == null && uri.fragment == null
    return text.takeIf { valid }
}
