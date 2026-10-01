package dev.denlogv.lexislearned.ui.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A file chosen by the user.
 *
 * @property name the file's display name, or null if unknown.
 * @property bytes the file contents.
 */
class FileContent(val name: String?, val bytes: ByteArray)

/** Reads and writes the files the user picks; tests replace it with an in-memory fake. */
interface DeckFiles {
    /**
     * Reads a file.
     *
     * @param uri the file's address as given by the file picker.
     * @return its name and contents.
     * @throws IOException if the file cannot be opened.
     */
    suspend fun read(uri: String): FileContent

    /**
     * Writes a file.
     *
     * @param uri the file's address as given by the file picker.
     * @param write writes the contents to the stream.
     * @throws IOException if the file cannot be opened for writing.
     */
    suspend fun write(uri: String, write: (OutputStream) -> Unit)
}

/**
 * [DeckFiles] for content URIs handed out by Android's file picker.
 *
 * @param context used to reach the content resolver.
 */
class ContentResolverDeckFiles(private val context: Context) : DeckFiles {
    /**
     * Reads a file through the content resolver.
     *
     * @param uri the content URI.
     * @return its display name and contents.
     * @throws IOException if the file cannot be opened.
     */
    override suspend fun read(uri: String): FileContent = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(uri)
        val bytes = context.contentResolver.openInputStream(parsed)?.use { it.readBytes() } ?: throw IOException("Cannot open the file")
        FileContent(displayName(parsed), bytes)
    }

    /**
     * Writes a file through the content resolver.
     *
     * @param uri the content URI.
     * @param write writes the contents to the stream.
     * @throws IOException if the file cannot be opened for writing.
     */
    override suspend fun write(uri: String, write: (OutputStream) -> Unit) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(Uri.parse(uri)) ?: throw IOException("Cannot write the file")
        stream.use(write)
    }

    /**
     * The file's display name.
     *
     * @param uri the content URI.
     * @return the name, or null if the provider does not give one.
     */
    private fun displayName(uri: Uri): String? = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
    }
}
