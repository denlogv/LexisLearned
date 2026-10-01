package dev.denlogv.lexislearned.epub

/** Helpers for the slash-separated paths and links used inside an EPUB archive. */
internal object EpubPaths {
    /**
     * The folder part of a path.
     *
     * @param path a path inside the archive.
     * @return everything before the last slash, or an empty string for a top-level file.
     */
    fun directoryOf(path: String): String = path.substringBeforeLast('/', "")

    /**
     * Turns a link found in a document into a path in the archive.
     *
     * @param dir the folder of the document that contains the link.
     * @param href the link; percent-encoding and a `#fragment` are allowed.
     * @return the normalised path, without the fragment and with `.` and `..` resolved.
     */
    fun resolve(dir: String, href: String): String {
        val relative = HtmlText.percentDecode(href.substringBefore('#'))
        return normalize(if (dir.isEmpty()) relative else "$dir/$relative")
    }

    /**
     * The anchor part of a link.
     *
     * @param href a link such as `chapter1.xhtml#start`.
     * @return the text after `#`, or null if there is none.
     */
    fun fragmentOf(href: String): String? = href.substringAfter('#', "").ifEmpty { null }

    /**
     * Resolves `.` and `..` segments and removes empty ones.
     *
     * @param path a slash-separated path.
     * @return the simplified path.
     */
    private fun normalize(path: String): String {
        val out = ArrayList<String>()
        for (part in path.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (out.isNotEmpty()) out.removeAt(out.size - 1)
                else -> out += part
            }
        }
        return out.joinToString("/")
    }
}
