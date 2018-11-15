package be.geoffrey.xmlweave.core.usecase

import be.geoffrey.xmlweave.core.usecase.xmlrendering.XmlRenderer
import com.geoffrey.xmlweave.xmlschema.LocalElement
import com.geoffrey.xmlweave.xmlschema.Schema
import com.geoffrey.xmlweave.xmlschema.TopLevelComplexType
import com.geoffrey.xmlweave.xmlschema.TopLevelElement
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBElement

@Service
class XmlWeaveInteractor : XmlWeaveService {

    override fun renderElementAsXml(e: Element): String {
        return XmlRenderer().renderAsXml(e)
    }

    override fun getElementStructure(xsdFile: File, rootName: String?): Optional<Element> {

        if (rootName == null || rootName.isBlank()) {
            return Optional.empty()
        }

        val schema = JAXB.unmarshal(xsdFile, Schema::class.java)

        val complexTypesInSchema = schema.simpleTypeOrComplexTypeOrGroup
                .filter { it is TopLevelComplexType }
                .map { it as TopLevelComplexType }
                .associateBy({ it.name }, { it })

        val topLevelElement = findAllTopLevelElements(schema).firstOrNull { it -> it.name == rootName }
                ?: return Optional.empty()

        if (topLevelElement.type != null) {
            val complexType = complexTypesInSchema[topLevelElement.type.localPart]
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
                .map { it -> Element(it.name) }
    }
}

private fun findAllTopLevelElements(schema: Schema): List<TopLevelElement> {
    return schema.simpleTypeOrComplexTypeOrGroup
            .filter { e -> e is TopLevelElement }
            .map { e -> e as TopLevelElement }
}