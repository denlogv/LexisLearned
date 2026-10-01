package dev.denlogv.lexislearned.ai

import java.util.Locale

/** Language names for prompts and screens. */
object Languages {
    /**
     * The English name of a language.
     *
     * @param code an ISO 639-1 code such as "fr".
     * @return the name such as "French", or the code itself if it is not a known language.
     */
    fun nameOf(code: String): String = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH).ifBlank { code }
}
