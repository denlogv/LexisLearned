package dev.denlogv.lexislearned.ai

/** Cuts long chapter text into pieces that stay comfortably inside a model's limits. */
internal object TextSplitter {
    /**
     * Splits text at line breaks into pieces of at most about [max] characters (a single longer line stays whole).
     *
     * @param text the text.
     * @param max the target maximum length of a piece in characters.
     * @return the pieces in order; a single piece if the text is short enough.
     */
    fun split(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val parts = ArrayList<String>()
        val current = StringBuilder()
        for (line in text.split("\n")) {
            if (current.length + line.length > max && current.isNotEmpty()) {
                parts += current.toString()
                current.clear()
            }
            current.append(line).append('\n')
        }
        if (current.isNotBlank()) parts += current.toString()
        return parts
    }
}
