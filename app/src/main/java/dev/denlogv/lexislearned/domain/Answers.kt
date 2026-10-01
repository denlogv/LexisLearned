package dev.denlogv.lexislearned.domain

import java.text.Normalizer

/** Forgiving comparison of typed answers with the expected translation. */
object Answers {
    private const val TYPO_MIN_LENGTH = 6
    private val brackets = Regex("\\(.*?\\)")
    private val combiningMarks = Regex("\\p{Mn}+")
    private val punctuation = Regex("[^\\p{L}\\p{N} ]")
    private val whitespace = Regex("\\s+")

    /**
     * Whether a typed answer counts as correct.
     *
     * Case, accents, punctuation, brackets and a leading "to " are ignored; any one of several comma, semicolon
     * or slash separated translations is accepted; words of six or more letters may contain one typo.
     *
     * @param input what the learner typed.
     * @param expected the stored answer, possibly listing several translations.
     * @return true if [input] matches an accepted answer.
     */
    fun isCorrect(input: String, expected: String): Boolean {
        val typed = normalize(input)
        if (typed.isEmpty()) return false
        val candidates = alternatives(expected) + normalize(expected)
        return candidates.any { it == typed || (it.length >= TYPO_MIN_LENGTH && distance(it, typed) <= 1) }
    }

    /**
     * The separate answers contained in a stored answer.
     *
     * @param expected the stored answer, for example "to run, to jog (fast)".
     * @return each alternative in normalised form, without bracketed parts and empty items.
     */
    fun alternatives(expected: String): List<String> = expected.replace(brackets, "")
        .split(',', ';', '/')
        .map { normalize(it) }
        .filter { it.isNotEmpty() }

    /**
     * Reduces text to a comparable form: lower case, no accents or punctuation, single spaces, no leading "to ".
     *
     * @param s the text to normalise.
     * @return the normalised text.
     */
    fun normalize(s: String): String {
        val decomposed = Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
        val stripped = decomposed.replace(combiningMarks, "")
            .replace(punctuation, " ")
            .replace(whitespace, " ")
            .trim()
        return stripped.removePrefix("to ").trim()
    }

    /**
     * Levenshtein edit distance between two strings.
     *
     * @param a the first string.
     * @param b the second string.
     * @return the minimum number of single-character insertions, deletions and substitutions turning [a] into [b].
     */
    private fun distance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            previous = editRow(a, b, i, previous)
        }
        return previous[b.length]
    }

    /**
     * One row of the Levenshtein table.
     *
     * @param a the first string.
     * @param b the second string.
     * @param i the 1-based index of the row, that is the prefix length of [a].
     * @param previous the previous row.
     * @return the row for prefix length [i].
     */
    private fun editRow(a: String, b: String, i: Int, previous: IntArray): IntArray {
        val row = IntArray(b.length + 1)
        row[0] = i
        for (j in 1..b.length) {
            val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
            row[j] = minOf(previous[j] + 1, row[j - 1] + 1, substitution)
        }
        return row
    }
}
