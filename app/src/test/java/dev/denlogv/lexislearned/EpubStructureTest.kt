package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.epub.EpubStructure
import dev.denlogv.lexislearned.epub.EpubStructure.Leaf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpubStructureTest {
    private fun leaves(vararg t: Pair<String, Int>) = t.map { Leaf(it.first, it.second) }

    @Test
    fun chapterNumbers() {
        assertEquals(12, EpubStructure.chapterNumber("Chapter 12"))
        assertEquals(12, EpubStructure.chapterNumber("12: Anchors"))
        assertEquals(1, EpubStructure.chapterNumber("01 Chapter 1"))
        assertNull(EpubStructure.chapterNumber("Introduction"))
    }

    @Test
    fun skipReasons() {
        assertEquals("front/back matter", EpubStructure.skipReason("Notes", 16000, false))
        assertEquals("front/back matter", EpubStructure.skipReason("Copyright Page", 300, false))
        assertEquals("Project Gutenberg boilerplate", EpubStructure.skipReason("Anything", 3000, true))
        assertEquals("very short (40 words)", EpubStructure.skipReason("Chapter 29", 40, false))
        assertNull(EpubStructure.skipReason("Chapter 3", 2000, false))
    }

    @Test
    fun partTitlesGroupTheFollowingChapters() { // continuous chapter numbering across parts, with separate "Part N" pages
        val l = leaves(
            "Introduction" to 5000,
            "Part 1: First topic" to 10,
            "1: Chapter title" to 4000,
            "2: Another title" to 3000,
            "Part 2: Second topic" to 10,
            "3: Third title" to 4000,
            "Conclusions" to 4000,
        )
        assertEquals(listOf(null, 1, 1, 1, 4, 4, 4), EpubStructure.detectHeaders(l))
    }

    @Test
    fun numberingRestartsMakeGroupsInAnOmnibus() { // an omnibus: "Chapter 1" restarts after each book title
        val l = leaves(
            "Foreword" to 1200,
            "Book A" to 620,
            "Chapter 1" to 900,
            "Chapter 2" to 900,
            "Book B" to 20,
            "Chapter 1" to 900,
            "Chapter 2" to 900,
        )
        assertEquals(listOf(null, 1, 1, 1, 4, 4, 4), EpubStructure.detectHeaders(l))
    }

    @Test
    fun singleNumberedRunIsNotSplit() {
        val l = leaves("Prologue" to 500, "Chapter 1" to 900, "Chapter 2" to 900)
        assertEquals(listOf<Int?>(null, null, null), EpubStructure.detectHeaders(l))
    }
}
