package dev.denlogv.lexislearned.ui

import dev.denlogv.lexislearned.ui.library.DeckFiles
import dev.denlogv.lexislearned.ui.library.FileContent
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/** Files kept in memory. [read] fails for unknown addresses, like a missing file would. */
class MemoryFiles(val files: MutableMap<String, ByteArray> = HashMap()) : DeckFiles {
    override suspend fun read(uri: String): FileContent = FileContent(uri, files[uri] ?: throw IOException("missing $uri"))

    override suspend fun write(uri: String, write: (OutputStream) -> Unit) {
        files[uri] = ByteArrayOutputStream().also(write).toByteArray()
    }
}
