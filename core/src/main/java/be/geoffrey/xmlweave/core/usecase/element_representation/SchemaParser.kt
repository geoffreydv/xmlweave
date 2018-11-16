package be.geoffrey.xmlweave.core.usecase.element_representation

import be.geoffrey.xmlweave.xmlschema.ComplexType
import be.geoffrey.xmlweave.xmlschema.Schema
import be.geoffrey.xmlweave.xmlschema.TopLevelComplexType
import be.geoffrey.xmlweave.xmlschema.TopLevelElement
import org.springframework.stereotype.Service
import java.io.File
import javax.xml.bind.JAXB

data class SchemaMetadata(private val complexTypes: Map<String, ComplexType>,
                          private val elements: List<TopLevelElement>) {

    fun complexType(name: String): ComplexType? = complexTypes[name]

    fun getElement(name: String): TopLevelElement? = elements.firstOrNull { it -> it.name == name }
}

@Service
class SchemaParser {
    fun extractSchemaMetadataFromXsd(xsdFile: File): SchemaMetadata {

        val schema = JAXB.unmarshal(xsdFile, Schema::class.java)

        val complexTypesInSchema = schema.simpleTypeOrComplexTypeOrGroup
                .filter { it is TopLevelComplexType }
                .map { it as TopLevelComplexType }
                .associateBy({ it.name }, { it })

        val elements = schema.simpleTypeOrComplexTypeOrGroup
                .filter { e -> e is TopLevelElement }
                .map { e -> e as TopLevelElement }

        return SchemaMetadata(complexTypesInSchema, elements)
    }

}