package com.xmlweave.core.xmlrendering

import com.xmlweave.core.element_representation.Attribute
import com.xmlweave.core.element_representation.Element
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory

object XmlRenderer {

    fun renderAsXml(el: Element, xmlDeclaration: Boolean = false): String {
        return prettyPrint(renderElement(el), xmlDeclaration).trim()
    }

    private fun renderElement(el: Element): String {
        var representation: String

        return if (el.isLeaf()) {
            "<${renderTagContent(el)} ${renderAttributes(el)} />"
        } else {
            representation = "<${renderTagContent(el)} ${renderAttributes(el)}>"
            for (child in el.children) {
                representation += renderElement(child)
            }
            representation += "</${renderTagContent(el)}>"
            representation
        }
    }

    private fun renderAttributes(el: Element): String {
        return el.attributes.joinToString(" ") { renderAttribute(it) }
    }

    private fun renderAttribute(attribute: Attribute): String {
        return "${attribute.name}=\"${attribute.value}\""
    }

    private fun renderTagContent(el: Element): String {
        val hasPrefix = el.prefix?.isNotBlank() ?: false
        return (if (hasPrefix) el.prefix.plus(":") else "").plus(el.name)
    }

    private fun prettyPrint(xml: String, xmlDeclaration: Boolean): String {

        val src = InputSource(StringReader(xml))
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src).documentElement

        val registry = DOMImplementationRegistry.newInstance()
        val impl = registry.getDOMImplementation("LS") as DOMImplementationLS

        val stringWriter = StringWriter()

        val lsOutput = impl.createLSOutput()
        lsOutput.encoding = "UTF-8"
        lsOutput.characterStream = stringWriter

        val writer = impl.createLSSerializer()
        writer.domConfig.setParameter("format-pretty-print", true)
        writer.domConfig.setParameter("xml-declaration", xmlDeclaration)
        writer.write(document, lsOutput)
        return stringWriter.toString()
    }
}
