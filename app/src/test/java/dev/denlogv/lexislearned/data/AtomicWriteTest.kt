package dev.denlogv.lexislearned.data

import java.io.File
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AtomicWriteTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun theDirectoryIsCreatedAndTheBytesAreWritten() {
        val target = File(folder.root, "a/b/file.bin")
        writeAtomically(target, byteArrayOf(1, 2))
        assertArrayEquals(byteArrayOf(1, 2), target.readBytes())
    }

    @Test
    fun anExistingFileIsReplacedAndNoTemporaryFileStaysBehind() {
        val target = File(folder.root, "file.bin")
        writeAtomically(target, byteArrayOf(1))
        writeAtomically(target, byteArrayOf(2, 3))
        assertArrayEquals(byteArrayOf(2, 3), target.readBytes())
        assertEquals(listOf("file.bin"), folder.root.list()!!.toList())
    }

    @Test(expected = IOException::class)
    fun aTargetInsideAFileCannotBeWritten() {
        val blocker = folder.newFile("blocker")
        writeAtomically(File(blocker, "file.bin"), byteArrayOf(1))
    }
}
