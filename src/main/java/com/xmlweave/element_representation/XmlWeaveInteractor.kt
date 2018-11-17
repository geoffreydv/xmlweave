package com.xmlweave.element_representation

import com.xmlweave.xmlrendering.XmlRenderer
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.xml.bind.JAXBElement

@Service
internal class XmlWeaveInteractor(private val xsdFile: XsdFile) : XmlWeaveService {

    override fun renderElementAsXml(e: Element): String {
        return XmlRenderer().renderAsXml(e, true)
    }

    override fun getElementStructure(xsdFile: File, elementName: String?): Optional<Element> {

        if (elementName == null || elementName.isBlank()) {
            return Optional.empty()
        }

        val metadata = this.xsdFile.parse(xsdFile)
        val topLevelElement = metadata.getElement(elementName) ?: return Optional.empty()

        return Optional.of(representElement(topLevelElement, metadata))
    }

    private fun representElement(element: Element2,
                                 metadata: Schema): Element {

        val elementName = element.name!!

        if (element.type != null) {
            val complexType = metadata.getComplexType(element.type.localPart)
            val children = extractChildElementsFromComplexType(complexType, metadata)
            return Element(elementName, children)
        } else if (element.complexType != null) {
            val complexType = element.complexType
            val children = extractChildElementsFromComplexType(complexType, metadata)
            return Element(elementName, children)
        }

        return Element(element.name)
    }

    private fun extractChildElementsFromComplexType(complexType: ComplexType?,
                                                    metadata: Schema): List<Element> {
        val subElements = complexType?.sequence?.particle ?: return listOf()
        return subElements
                .filter { it is LocalElement }
                .map { it as LocalElement}
                .map { it -> representElement(it, metadata) }
    }
}