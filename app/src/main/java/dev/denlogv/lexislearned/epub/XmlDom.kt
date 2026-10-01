package dev.denlogv.lexislearned.epub

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Parses an XML document without loading external DTDs.
 *
 * @param bytes the document.
 * @return the root element.
 */
internal fun parseXml(bytes: ByteArray): Element = DocumentBuilderFactory.newInstance().apply {
    isNamespaceAware = false
    runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
}.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).documentElement

/**
 * Whether an element has the given name, ignoring any namespace prefix.
 *
 * @param name the name without prefix, for example "title" matches both `title` and `dc:title`.
 * @return true if the names match.
 */
private fun Element.isNamed(name: String): Boolean = tagName == name || tagName.substringAfter(':') == name

/**
 * The direct child elements with a given name.
 *
 * @param name the element name, ignoring any namespace prefix.
 * @return the matching children in document order.
 */
internal fun Element.childElements(name: String): List<Element> {
    val out = ArrayList<Element>()
    var child: Node? = firstChild
    while (child != null) {
        if (child is Element && child.isNamed(name)) out += child
        child = child.nextSibling
    }
    return out
}

/**
 * All descendant elements with a given name, at any depth.
 *
 * @param name the element name, ignoring any namespace prefix.
 * @return the matching elements in document order.
 */
internal fun Element.descendants(name: String): List<Element> {
    val out = ArrayList<Element>()
    collectDescendants(this, name, out)
    return out
}

/**
 * Recursive helper of [descendants].
 *
 * @param node the node whose descendants are searched.
 * @param name the element name to look for.
 * @param out receives the matches.
 */
private fun collectDescendants(node: Node, name: String, out: MutableList<Element>) {
    var child: Node? = node.firstChild
    while (child != null) {
        if (child is Element) {
            if (child.isNamed(name)) out += child
            collectDescendants(child, name, out)
        }
        child = child.nextSibling
    }
}
