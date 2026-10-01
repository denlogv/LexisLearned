package dev.denlogv.lexislearned.epub

import java.io.ByteArrayOutputStream

/**
 * Dependency-free HTML to plain text conversion for EPUB chapter files. It is a single linear scan without regular
 * expressions, because books are megabytes of markup and regex passes are very slow on Android.
 */
object HtmlText {
    private val blockTags = setOf(
        "p", "div", "br", "li", "h1", "h2", "h3", "h4", "h5", "h6", "tr", "blockquote", "section", "article", "pre",
    )
    private val skipTags = setOf("script", "style", "head", "nav")
    private val named = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "mdash" to "—", "ndash" to "–", "hellip" to "…", "lsquo" to "‘", "rsquo" to "’",
        "ldquo" to "“", "rdquo" to "”", "shy" to "",
    )
    private val whitespaceRun = Regex("\\s+")
    private val heading = Regex("(?is)<h[1-3][^>]*>(.*?)</h[1-3]>")

    /** Longest entity name (between `&` and `;`) that is decoded, to avoid treating a stray ampersand as an entity. */
    private const val MAX_ENTITY_LENGTH = 10

    /**
     * Converts HTML to plain text: tags removed, block elements become line breaks, entities decoded, whitespace collapsed.
     * Script, style, head and navigation elements are dropped with their content.
     *
     * @param html the markup, possibly a fragment that starts or ends inside an element.
     * @return the readable text.
     */
    fun toPlain(html: String): String {
        val raw = StringBuilder(html.length / 2)
        var i = 0
        while (i < html.length) {
            i = when (html[i]) {
                '<' -> consumeTag(html, i, raw)
                '&' -> consumeEntity(html, i, raw)
                else -> {
                    raw.append(html[i])
                    i + 1
                }
            }
        }
        return collapse(raw)
    }

    /**
     * Collapses every run of whitespace into a single space and trims the ends.
     *
     * @param text the text.
     * @return the tidied text.
     */
    fun squashSpaces(text: String): String = text.replace(whitespaceRun, " ").trim()

    /**
     * The text of the first heading (`h1` to `h3`) in a document.
     *
     * @param html the markup.
     * @return the heading text on one line, or null if there is no non-empty heading.
     */
    fun firstHeading(html: String): String? =
        heading.find(html)?.groupValues?.get(1)?.let { toPlain(it).replace('\n', ' ').trim() }?.takeIf { it.isNotEmpty() }

    /**
     * Counts words, that is runs of letters, digits or underscores.
     *
     * @param text the text.
     * @return the number of words.
     */
    fun countWords(text: String): Int {
        var count = 0
        var inWord = false
        for (ch in text) {
            val isWordChar = ch.isLetterOrDigit() || ch == '_'
            if (isWordChar && !inWord) count++
            inWord = isWordChar
        }
        return count
    }

    /**
     * Decodes `%XX` escapes (UTF-8) as found in links.
     *
     * @param s the encoded text; invalid escapes are kept as they are.
     * @return the decoded text.
     */
    fun percentDecode(s: String): String {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val hex = if (s[i] == '%' && i + 3 <= s.length) s.substring(i + 1, i + 3).toIntOrNull(16) else null
            if (hex != null) {
                out.write(hex)
                i += 3
            } else {
                val cp = s.codePointAt(i)
                out.write(String(Character.toChars(cp)).toByteArray())
                i += Character.charCount(cp)
            }
        }
        return out.toByteArray().toString(Charsets.UTF_8)
    }

    /**
     * Handles a `<` at [start]: a comment, a skipped element, a block tag (adds a line break) or any other tag (dropped).
     *
     * @param html the markup.
     * @param start the index of the `<`.
     * @param raw the output so far; a line break is appended for block tags.
     * @return the index to continue from.
     */
    private fun consumeTag(html: String, start: Int, raw: StringBuilder): Int {
        if (html.startsWith("<!--", start)) return skipComment(html, start)
        val close = html.indexOf('>', start + 1)
        if (close < 0) return html.length
        val tag = parseTag(html, start, close)
        if (!tag.closing && tag.name in skipTags && html[close - 1] != '/') return skipElement(html, tag.name, close)
        if (tag.name in blockTags) raw.append('\n')
        return close + 1
    }

    /**
     * A parsed tag.
     *
     * @property name the lower-case tag name.
     * @property closing true for an end tag such as `</p>`.
     */
    private class Tag(val name: String, val closing: Boolean)

    /**
     * Reads the name of the tag spanning `[start, close]`.
     *
     * @param html the markup.
     * @param start the index of `<`.
     * @param close the index of `>`.
     * @return the tag.
     */
    private fun parseTag(html: String, start: Int, close: Int): Tag {
        var k = start + 1
        val closing = k < close && html[k] == '/'
        if (closing) k++
        val nameStart = k
        while (k < close && !html[k].isWhitespace() && html[k] != '/') k++
        return Tag(html.substring(nameStart, k).lowercase(), closing)
    }

    /**
     * Skips an HTML comment.
     *
     * @param html the markup.
     * @param start the index of `<!--`.
     * @return the index after the comment, or the end of the text if it is never closed.
     */
    private fun skipComment(html: String, start: Int): Int {
        val end = html.indexOf("-->", start + 4)
        return if (end < 0) html.length else end + 3
    }

    /**
     * Skips an element together with its content, for example a script.
     *
     * @param html the markup.
     * @param name the element name.
     * @param openEnd the index of the `>` of the opening tag.
     * @return the index after the closing tag, or the end of the text if it is never closed.
     */
    private fun skipElement(html: String, name: String, openEnd: Int): Int {
        val end = html.indexOf("</$name", openEnd, ignoreCase = true)
        if (end < 0) return html.length
        val endClose = html.indexOf('>', end)
        return if (endClose < 0) html.length else endClose + 1
    }

    /**
     * Handles a `&` at [start]: decodes a known entity, otherwise keeps the ampersand.
     *
     * @param html the markup.
     * @param start the index of the `&`.
     * @param raw the output so far.
     * @return the index to continue from.
     */
    private fun consumeEntity(html: String, start: Int, raw: StringBuilder): Int {
        val semi = html.indexOf(';', start + 1)
        val decoded = if (semi in (start + 2)..(start + MAX_ENTITY_LENGTH)) decodeEntity(html.substring(start + 1, semi)) else null
        if (decoded == null) {
            raw.append('&')
            return start + 1
        }
        raw.append(decoded)
        return semi + 1
    }

    /**
     * Decodes one entity.
     *
     * @param entity the text between `&` and `;`, for example `amp`, `#8212` or `#x2014`.
     * @return the character, or null if the entity is not known.
     */
    private fun decodeEntity(entity: String): String? = when {
        entity.startsWith("#x") || entity.startsWith("#X") -> entity.drop(2).toIntOrNull(16)?.let(::codePoint)
        entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.let(::codePoint)
        else -> named[entity]
    }

    /**
     * Converts a code point to a string.
     *
     * @param cp the code point.
     * @return the character, or null if it is not a valid code point.
     */
    private fun codePoint(cp: Int): String? = if (Character.isValidCodePoint(cp)) String(Character.toChars(cp)) else null

    /**
     * Collapses runs of spaces, trims around line breaks and limits blank lines to one.
     *
     * @param s the text with raw whitespace.
     * @return the tidied text.
     */
    private fun collapse(s: CharSequence): String {
        val out = StringBuilder(s.length)
        var pendingSpace = false
        var newlines = 0
        for (ch in s) {
            when {
                ch == '\n' -> {
                    newlines++
                    pendingSpace = false
                }
                isSpace(ch) -> if (out.isNotEmpty() && newlines == 0) pendingSpace = true
                else -> {
                    appendSeparator(out, newlines, pendingSpace)
                    newlines = 0
                    pendingSpace = false
                    out.append(ch)
                }
            }
        }
        return out.toString()
    }

    /**
     * Whether a character is a horizontal space.
     *
     * @param ch the character.
     * @return true for space, tab, carriage return and no-break space.
     */
    private fun isSpace(ch: Char): Boolean = ch == ' ' || ch == '\t' || ch == ' ' || ch == '\r'

    /**
     * Appends the separator that belongs before the next visible character.
     *
     * @param out the output.
     * @param newlines how many line breaks were seen since the last visible character.
     * @param pendingSpace whether a space was seen since then.
     */
    private fun appendSeparator(out: StringBuilder, newlines: Int, pendingSpace: Boolean) {
        if (newlines > 0 && out.isNotEmpty()) {
            out.append(if (newlines >= 2) "\n\n" else "\n")
        } else if (pendingSpace) {
            out.append(' ')
        }
    }
}
