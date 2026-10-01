package dev.denlogv.lexislearned.ai

/**
 * An error from an LLM provider.
 *
 * @param message what went wrong, phrased for the user.
 * @property code the HTTP status code, or 0 if the error did not come from an HTTP response.
 * @param cause the underlying error, if any.
 */
class LlmException(message: String, val code: Int = 0, cause: Throwable? = null) : Exception(message, cause)

/** A chat-completion style LLM. The caller supplies the user's own API key when creating a client. */
interface LlmClient {
    /**
     * Sends a system and a user message and returns the model's reply.
     *
     * @param system the system prompt.
     * @param user the user message.
     * @return the reply text.
     * @throws LlmException if the request fails or the reply is empty.
     */
    suspend fun complete(system: String, user: String): String
}
