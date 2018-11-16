package be.geoffrey.xmlweave.core.usecase.element_representation

import be.geoffrey.xmlweave.core.usecase.XmlWeaveService
import be.geoffrey.xmlweave.core.usecase.xmlrendering.XmlRenderer
import be.geoffrey.xmlweave.xmlschema.ComplexType
import be.geoffrey.xmlweave.xmlschema.LocalElement
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.xml.bind.JAXBElement

@Service
class XmlWeaveInteractor(private val parser: SchemaParser) : XmlWeaveService {

    override fun renderElementAsXml(e: Element): String {
        return XmlRenderer().renderAsXml(e)
    }

    override fun getElementStructure(xsdFile: File, elementName: String?): Optional<Element> {

        if (elementName == null || elementName.isBlank()) {
            return Optional.empty()
        }

        val metadata = parser.extractSchemaMetadataFromXsd(xsdFile)
        val topLevelElement = metadata.getElement(elementName) ?: return Optional.empty()
        return Optional.of(representElement(topLevelElement, metadata))
    }

    private fun representElement(element: be.geoffrey.xmlweave.xmlschema.Element,
                                 metadata: SchemaMetadata): Element {

        val elementName = element.name

        if (element.type != null) {
            val complexType = metadata.complexType(element.type.localPart)
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
                                                    metadata: SchemaMetadata): List<Element> {
        val subElements = complexType?.sequence?.particle ?: return listOf()
        return subElements
                .map { it as JAXBElement<*> }
                .filter { it.value is LocalElement }
                .map { it.value as LocalElement }
                .map { it -> representElement(it, metadata) }
    }
}