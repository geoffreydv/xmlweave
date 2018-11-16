package be.geoffrey.xmlweave.core.usecase.xmlrendering

import be.geoffrey.xmlweave.core.usecase.element_representation.Element
import org.springframework.stereotype.Service
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

@Service
class XmlRenderer {

    fun renderAsXml(el: Element): String {
        return prettyPrint(renderElement(el)).trim()
    }

    private fun renderElement(el: Element): String {
        var representation: String

        if (el.isLeaf()) {
            return "<${el.name} />"
        } else {
            representation = "<${el.name}>"
            for (child in el.children) {
                representation += renderElement(child)
            }
            representation += "</${el.name}>"
            return representation
        }
    }

    private fun prettyPrint(xml: String): String {

        val src = InputSource(StringReader(xml))
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src).documentElement

        val registry = DOMImplementationRegistry.newInstance()
        val impl = registry.getDOMImplementation("LS") as DOMImplementationLS
        val writer = impl.createLSSerializer()
        writer.domConfig.setParameter("format-pretty-print", true)
        writer.domConfig.setParameter("xml-declaration", false)

        return writer.writeToString(document)
    }
}
