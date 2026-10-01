package dev.denlogv.lexislearned.epub

import dev.denlogv.lexislearned.format.FormatException
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * The files of an EPUB (a zip archive) held in memory.
 *
 * @param files the archive entries by path.
 */
internal class EpubArchive private constructor(private val files: Map<String, ByteArray>) {
    /**
     * Looks up a file.
     *
     * @param path the path inside the archive.
     * @return the file's bytes, or null if there is no such file.
     */
    fun bytes(path: String): ByteArray? = files[path]

    /**
     * Looks up a text file.
     *
     * @param path the path inside the archive.
     * @return the file decoded as UTF-8, or null if there is no such file.
     */
    fun text(path: String): String? = files[path]?.toString(Charsets.UTF_8)

    /**
     * Whether the archive has a file.
     *
     * @param path the path inside the archive.
     * @return true if the file exists.
     */
    fun contains(path: String): Boolean = path in files

    companion object {
        private const val DRM_FILE = "META-INF/encryption.xml"

        /**
         * Reads all entries of an EPUB.
         *
         * @param input the EPUB file contents; closed when done.
         * @return the archive.
         * @throws FormatException if the book is DRM protected.
         */
        fun read(input: InputStream): EpubArchive {
            val files = HashMap<String, ByteArray>()
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) files[entry.name] = zip.readBytes()
                }
            }
            rejectDrm(files)
            return EpubArchive(files)
        }

        /**
         * Fails for books with encrypted content, which cannot be read.
         *
         * @param files the archive entries.
         * @throws FormatException if the archive declares encrypted data.
         */
        private fun rejectDrm(files: Map<String, ByteArray>) {
            val encryption = files[DRM_FILE] ?: return
            if (String(encryption).contains("EncryptedData")) throw FormatException("This EPUB is DRM protected")
        }
    }
}
