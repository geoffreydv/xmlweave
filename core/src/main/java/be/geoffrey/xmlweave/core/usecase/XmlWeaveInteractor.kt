package be.geoffrey.xmlweave.core.usecase

import com.geoffrey.xmlweave.xmlschema.LocalElement
import com.geoffrey.xmlweave.xmlschema.Schema
import com.geoffrey.xmlweave.xmlschema.TopLevelElement
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBElement

@Service
class XmlWeaveInteractor : XmlWeaveService {

    override fun getRepresentation(xsdFile: File, rootName: String?): Optional<Element> {

        if (rootName == null || rootName.isBlank()) {
            return Optional.empty()
        }

        val schema = JAXB.unmarshal(xsdFile, Schema::class.java)

        val topLevelElement = findAllTopLevelElements(schema).firstOrNull { it -> it.name == rootName }
                ?: return Optional.empty()

        val maybeSubElements = topLevelElement.complexType?.sequence?.particle
                ?: return Optional.of(Element(rootName))

        val subElements = maybeSubElements
                .map { it as JAXBElement<*> }
                .filter { it.value is LocalElement }
                .map { it.value as LocalElement }
                .map { it -> Element(it.name) }

        return Optional.of(Element(rootName, subElements))
    }
}

private fun findAllTopLevelElements(schema: Schema): List<TopLevelElement> {
    return schema.simpleTypeOrComplexTypeOrGroup
            .filter { e -> e is TopLevelElement }
            .map { e -> e as TopLevelElement }
}