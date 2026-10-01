package dev.denlogv.lexislearned.epub

import dev.denlogv.lexislearned.format.FormatException
import org.w3c.dom.Element

/**
 * What the EPUB package document (OPF) says about the book.
 *
 * @property title the book title, whitespace collapsed; may be empty.
 * @property language the declared language code; may be empty.
 * @property spine the paths of the HTML files in reading order.
 * @property ncxPath the path of the EPUB 2 table of contents, or null.
 * @property navPath the path of the EPUB 3 navigation document, or null.
 */
internal class PackageDocument(
    val title: String,
    val language: String,
    val spine: List<String>,
    val ncxPath: String?,
    val navPath: String?,
)

/** Reads the package document (OPF) of an EPUB. */
internal object PackageReader {
    private const val CONTAINER = "META-INF/container.xml"
    private const val NCX_TYPE = "application/x-dtbncx+xml"

    /**
     * Reads the package document.
     *
     * @param archive the opened EPUB.
     * @return the book's metadata, reading order and table-of-contents locations.
     * @throws FormatException if the archive is not an EPUB or its package document is missing.
     */
    fun read(archive: EpubArchive): PackageDocument {
        val opfPath = rootFilePath(archive)
        val opf = parseXml(archive.bytes(opfPath) ?: throw FormatException("Missing $opfPath"))
        val base = EpubPaths.directoryOf(opfPath)
        val manifest = opf.descendants("item").associateBy { it.getAttribute("id") }
        return PackageDocument(
            title = opf.descendants("title").firstOrNull()?.textContent.orEmpty().let(HtmlText::squashSpaces),
            language = opf.descendants("language").firstOrNull()?.textContent?.trim().orEmpty(),
            spine = spinePaths(opf, manifest, base, archive),
            ncxPath = ncxItem(opf, manifest)?.let { EpubPaths.resolve(base, it.getAttribute("href")) },
            navPath = navItem(manifest)?.let { EpubPaths.resolve(base, it.getAttribute("href")) },
        )
    }

    /**
     * Finds the package document through the container file.
     *
     * @param archive the opened EPUB.
     * @return the path of the OPF file.
     * @throws FormatException if the container file or its root file entry is missing.
     */
    private fun rootFilePath(archive: EpubArchive): String {
        val container = archive.bytes(CONTAINER) ?: throw FormatException("Not an EPUB (no container.xml)")
        return parseXml(container).descendants("rootfile").firstOrNull()?.getAttribute("full-path")
            ?: throw FormatException("EPUB has no package file")
    }

    /**
     * The HTML files in reading order.
     *
     * @param opf the package document.
     * @param manifest the manifest items by id.
     * @param base the folder of the package document.
     * @param archive the opened EPUB; files missing from it are skipped.
     * @return archive paths of the HTML files listed in the spine.
     */
    private fun spinePaths(opf: Element, manifest: Map<String, Element>, base: String, archive: EpubArchive): List<String> =
        opf.descendants("itemref").mapNotNull { ref ->
            val item = manifest[ref.getAttribute("idref")] ?: return@mapNotNull null
            if (!item.getAttribute("media-type").contains("html")) return@mapNotNull null
            EpubPaths.resolve(base, item.getAttribute("href")).takeIf { archive.contains(it) }
        }

    /**
     * The EPUB 2 table-of-contents item: the one named by the spine, else any NCX item.
     *
     * @param opf the package document.
     * @param manifest the manifest items by id.
     * @return the manifest item, or null if the book has no NCX.
     */
    private fun ncxItem(opf: Element, manifest: Map<String, Element>): Element? {
        val id = opf.descendants("spine").firstOrNull()?.getAttribute("toc").orEmpty()
        return manifest[id] ?: manifest.values.firstOrNull { it.getAttribute("media-type") == NCX_TYPE }
    }

    /**
     * The EPUB 3 navigation document item.
     *
     * @param manifest the manifest items by id.
     * @return the item flagged with the `nav` property, or null.
     */
    private fun navItem(manifest: Map<String, Element>): Element? =
        manifest.values.firstOrNull { "nav" in it.getAttribute("properties").split(' ') }
}
