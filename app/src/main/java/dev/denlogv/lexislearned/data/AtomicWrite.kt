package dev.denlogv.lexislearned.data

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Writes a file so that readers see either the old or the new contents, even if the process dies halfway: the bytes go to a
 * temporary file next to the target, which then replaces it in one step.
 *
 * @param target the file; its directory is created if it is missing.
 * @param bytes the new contents.
 * @throws IOException if the file cannot be written.
 */
fun writeAtomically(target: File, bytes: ByteArray) {
    val dir = target.absoluteFile.parentFile
    dir.mkdirs()
    val temp = File(dir, target.name + TEMP_SUFFIX)
    temp.writeBytes(bytes)
    Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
}

private const val TEMP_SUFFIX = ".tmp"
