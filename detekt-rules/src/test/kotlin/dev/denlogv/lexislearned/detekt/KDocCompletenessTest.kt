package dev.denlogv.lexislearned.detekt

import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.Assert.assertEquals
import org.junit.Test

class KDocCompletenessTest {
    private fun messages(code: String) = KDocCompleteness().compileAndLint(code).map { it.message }

    @Test
    fun fullyDocumentedCodePasses() {
        val code = """
            /**
             * A thing.
             *
             * @param name what it is called.
             * @property size how big it is.
             */
            class Thing(name: String, val size: Int) {
                /**
                 * Greets.
                 *
                 * @param who the person.
                 * @return the greeting.
                 */
                fun greet(who: String): String = "hi ${'$'}who"
            }
        """.trimIndent()
        assertEquals(emptyList<String>(), messages(code))
    }

    @Test
    fun undocumentedClassesObjectsAndFunctionsAreReported() {
        val code = """
            class A
            private object B
            interface C { fun d() }
            private fun e() = Unit
        """.trimIndent()
        assertEquals(
            listOf(
                "The class 'A' has no KDoc.",
                "The class 'B' has no KDoc.",
                "The class 'C' has no KDoc.",
                "The function 'd' has no KDoc.",
                "The function 'e' has no KDoc.",
            ),
            messages(code).sorted(),
        )
    }

    @Test
    fun missingParametersAreReported() {
        val code = """
            /**
             * A thing.
             *
             * @param a first.
             */
            class Thing(a: Int, b: Int)

            /**
             * Adds.
             *
             * @param x first.
             */
            fun add(x: Int, y: Int): Int = x + y
        """.trimIndent()
        assertEquals(
            listOf("The KDoc of 'Thing' does not document @param b.", "The KDoc of 'add' does not document @param y."),
            messages(code).sorted(),
        )
    }

    @Test
    fun localFunctionsAnonymousObjectsAndEnumEntriesAreSkipped() {
        val code = """
            /** Colours. */
            enum class Colour {
                RED,
                GREEN,
            }

            /** Holder. */
            class Holder {
                private companion object {
                    const val LIMIT = 3
                }
            }

            /** Runs. */
            fun run(): Any {
                fun helper() = 1
                return object {
                    val x = helper()
                }
            }
        """.trimIndent()
        assertEquals(emptyList<String>(), messages(code))
    }
}
