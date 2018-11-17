package com.xmlweave.xmlrendering

import com.xmlweave.element_representation.Element
import org.springframework.stereotype.Service
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory


@Service
class XmlRenderer {

    fun renderAsXml(el: Element, xmlDeclaration: Boolean = false): String {
        return prettyPrint(renderElement(el), xmlDeclaration).trim()
    }

    private fun renderElement(el: Element): String {
        var representation: String
        
        if (el.isLeaf()) {
            return "<${renderTagContent(el.prefix, el.name)} />"
        } else {
            representation = "<${renderTagContent(el.prefix, el.name)}>"
            for (child in el.children) {
                representation += renderElement(child)
            }
            representation += "</${renderTagContent(el.prefix, el.name)}>"
            return representation
        }
    }

    private fun renderTagContent(prefix: String?, name: String): String {
        val hasPrefix = prefix?.isNotBlank() ?: false
        return (if (hasPrefix) prefix.plus(":") else "").plus(name)
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
