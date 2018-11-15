package be.geoffrey.xmlweave.core.usecase.element_representation

import be.geoffrey.xmlweave.core.usecase.Element
import be.geoffrey.xmlweave.core.usecase.XmlWeaveService
import be.geoffrey.xmlweave.core.usecase.schema.SchemaParser
import be.geoffrey.xmlweave.core.usecase.xmlrendering.XmlRenderer
import com.geoffrey.xmlweave.xmlschema.LocalElement

import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.xml.bind.JAXBElement

@Service
class XmlWeaveInteractor(private val parser: SchemaParser) : XmlWeaveService {

    override fun renderElementAsXml(e: Element): String {
        return XmlRenderer().renderAsXml(e)
    }

    override fun getElementStructure(xsdFile: File, rootName: String?): Optional<Element> {

        if (rootName == null || rootName.isBlank()) {
            return Optional.empty()
        }

        val metadata = parser.extractSchemaMetadataFromXsd(xsdFile)
        val topLevelElement = metadata.getElement(rootName) ?: return Optional.empty()

        if (topLevelElement.type != null) {
            val complexType = metadata.complexType(topLevelElement.type.localPart)
            val subElements = complexType?.sequence?.particle ?: return Optional.of(Element(rootName))
            return Optional.of(Element(rootName, mapJaxbElementsToRepresentation(subElements)))
        } else {
            val subElements = topLevelElement.complexType?.sequence?.particle ?: return Optional.of(Element(rootName))
            return Optional.of(Element(rootName, mapJaxbElementsToRepresentation(subElements)))
        }
    }

    private fun mapJaxbElementsToRepresentation(maybeSubElements: List<Any>): List<Element> {
        return maybeSubElements
                .map { it as JAXBElement<*> }
                .filter { it.value is LocalElement }
                .map { it.value as LocalElement }
                .map { it -> representLocalElement(it) }
    }

    private fun representLocalElement(element: LocalElement): Element {
        if (element.complexType != null) {
            // Look it up!
        }

        return Element(element.name)
    }
}