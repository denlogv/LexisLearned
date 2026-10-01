package dev.denlogv.lexislearned.format

import dev.denlogv.lexislearned.domain.Deck

/** The deck formats the app understands, and how to pick one for a file. */
object FormatRegistry {
    /** All known formats. */
    val all: List<DeckFormat> = listOf(NativeFormat)

    /**
     * Finds the format for a file name.
     *
     * @param name the file name; may be null.
     * @return the format whose extension matches (ignoring case), or null if none does.
     */
    fun byExtension(name: String?): DeckFormat? {
        val ext = name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return all.firstOrNull { ext in it.extensions }
    }

    /**
     * Reads a deck, picking the format by file name and falling back to sniffing the content ('{' means JSON).
     *
     * @param fileName the file's name; may be null if unknown.
     * @param bytes the file contents.
     * @return the deck.
     * @throws FormatException if the file type is not supported or the contents are not valid.
     */
    fun read(fileName: String?, bytes: ByteArray): Deck {
        val format = byExtension(fileName) ?: NativeFormat.takeIf { looksLikeJson(bytes) }
            ?: throw FormatException("Unsupported file type. Import a .lexis file.")
        return format.read(bytes.inputStream())
    }

    /**
     * Whether the contents start with an opening brace, as JSON does.
     *
     * @param bytes the file contents.
     * @return true if the first byte is '{'.
     */
    private fun looksLikeJson(bytes: ByteArray): Boolean = bytes.firstOrNull()?.toInt()?.toChar() == '{'
}
