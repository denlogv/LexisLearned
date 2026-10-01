package dev.denlogv.lexislearned

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Filler text long enough to count as a real section. */
val lorem = "lorem ipsum dolor sit amet ".repeat(50)

/**
 * Builds a zip file from entries.
 *
 * @param entries pairs of entry name and text content.
 * @return the zip bytes.
 */
fun zipOf(vararg entries: Pair<String, String>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { z ->
        entries.forEach { (name, body) ->
            z.putNextEntry(ZipEntry(name))
            z.write(body.toByteArray())
            z.closeEntry()
        }
    }
    return out.toByteArray()
}

/** A small generated EPUB with a copyright page and two chapters, using an EPUB 3 navigation document. */
fun loremEpub(): ByteArray = zipOf(
    "META-INF/container.xml" to """<container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""",
    "OEBPS/content.opf" to """<package xmlns:dc="x"><metadata><dc:title>Lorem Book</dc:title><dc:language>en-GB</dc:language></metadata>
        <manifest><item id="nav" href="nav.xhtml" properties="nav" media-type="application/xhtml+xml"/>
        <item id="c1" href="ch%201.xhtml" media-type="application/xhtml+xml"/>
        <item id="c2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
        <item id="cp" href="copyright.xhtml" media-type="application/xhtml+xml"/></manifest>
        <spine><itemref idref="cp"/><itemref idref="c1"/><itemref idref="c2"/></spine></package>""",
    "OEBPS/nav.xhtml" to """<nav><ol><li><a href="copyright.xhtml">Copyright</a></li>
        <li><a href="ch%201.xhtml">Chapter One</a></li><li><a href="ch2.xhtml">Chapter Two</a></li></ol></nav>""",
    "OEBPS/ch 1.xhtml" to "<html><head><title>x</title></head><body><h1>Chapter One</h1><p>$lorem</p></body></html>",
    "OEBPS/ch2.xhtml" to "<html><body><p>$lorem &amp; more&nbsp;text</p></body></html>",
    "OEBPS/copyright.xhtml" to "<html><body><p>$lorem</p></body></html>",
)
