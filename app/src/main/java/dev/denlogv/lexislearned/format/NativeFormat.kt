package dev.denlogv.lexislearned.format

import dev.denlogv.lexislearned.domain.Deck
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** LexisLearned's own format: versioned JSON that carries everything, including study progress. */
object NativeFormat : DeckFormat {
    /** The newest file version this app can read and the one it writes. */
    const val SCHEMA_VERSION = 1

    override val id = "lexis"
    override val extensions = listOf("lexis")

    /**
     * The file's top level: a version number and the deck.
     *
     * @property schemaVersion the format version the file was written with.
     * @property deck the deck.
     */
    @Serializable
    private data class Envelope(val schemaVersion: Int, val deck: Deck)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Reads a `.lexis` file.
     *
     * @param input the file contents.
     * @return the deck.
     * @throws FormatException if the JSON is invalid or was written by a newer version of the app.
     */
    override fun read(input: InputStream): Deck {
        val envelope = try {
            json.decodeFromString<Envelope>(input.readBytes().toString(Charsets.UTF_8))
        } catch (e: kotlinx.serialization.SerializationException) {
            throw FormatException("Not a valid LexisLearned file", e)
        } catch (e: IllegalArgumentException) {
            throw FormatException("Not a valid LexisLearned file", e)
        }
        if (envelope.schemaVersion > SCHEMA_VERSION) {
            throw FormatException("File was created by a newer version of LexisLearned")
        }
        return envelope.deck
    }

    /**
     * Writes a `.lexis` file.
     *
     * @param deck the deck, including progress.
     * @param output where to write the JSON.
     */
    override fun write(deck: Deck, output: OutputStream) {
        output.write(json.encodeToString(Envelope.serializer(), Envelope(SCHEMA_VERSION, deck)).toByteArray())
    }
}
