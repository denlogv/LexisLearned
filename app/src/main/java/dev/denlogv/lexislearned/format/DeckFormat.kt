package dev.denlogv.lexislearned.format

import dev.denlogv.lexislearned.domain.Deck
import java.io.InputStream
import java.io.OutputStream

/**
 * Thrown when a file cannot be read or written in a deck format.
 *
 * @param message what went wrong, phrased for the user.
 * @param cause the underlying error, if any.
 */
class FormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** A file format that can be converted to and from the app's domain model. */
interface DeckFormat {
    /** Short stable identifier of the format, for example "lexis". */
    val id: String

    /** File extensions (lower case, without the dot) that belong to this format. */
    val extensions: List<String>

    /**
     * Reads a deck.
     *
     * @param input the file contents; not closed by this method.
     * @return the deck.
     * @throws FormatException if the contents are not valid in this format.
     */
    fun read(input: InputStream): Deck

    /**
     * Writes a deck.
     *
     * @param deck the deck to write.
     * @param output where to write the file contents; not closed by this method.
     * @throws UnsupportedOperationException if the format is read-only.
     * @throws FormatException if the deck cannot be represented in this format.
     */
    fun write(deck: Deck, output: OutputStream): Unit = throw UnsupportedOperationException("$id is read-only")
}
