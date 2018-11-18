package com.xmlweave.element_representation

import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.xml.namespace.QName

class CollectedInformation(val elements: Map<QName, Element2> = hashMapOf(),
                           val complexTypes: Map<QName, ComplexType> = hashMapOf()) {

    fun getElement(elementName: QName): Element2? {
        return elements[elementName]
    }

    fun getComplexType(name: QName): ComplexType? {
        return complexTypes[name]
    }
}

@Service
internal class XsdStructureServiceImpl(private val xsdFileParser: XsdFileParser) : XsdStructureService {

    override fun getElementStructure(xsdFile: File, elementToRender: QName?): Optional<Element> {

        if (elementToRender == null) {
            return Optional.empty()
        }

        val collectedElements = parseXsdFileAndReferencesToOtherFiles(xsdFile, CollectedInformation())

        val topLevelElement = collectedElements.getElement(elementToRender) ?: return Optional.empty()
        val topElement = representElement(topLevelElement, collectedElements)

        if (topLevelElement.name?.namespaceURI?.isNotBlank() == true) {
            return Optional.of(
                    Element(topElement.name,
                            topElement.children,
                            topElement.attributes.plus(Attribute("xmlns:root", topLevelElement.name.namespaceURI!!)),
                            topElement.value,
                            "root"))
        }
        return Optional.of(
                Element(topElement.name,
                        topElement.children,
                        topElement.attributes,
                        topElement.value)
        )
    }

    private fun parseXsdFileAndReferencesToOtherFiles(xsdFile: File,
                                                      collectedInformation: CollectedInformation): CollectedInformation {

        val xsdItems = this.xsdFileParser.parseSingleXsd(xsdFile)

        val updatedMetadata = CollectedInformation(
                collectedInformation.elements.plus(xsdItems.simpleTypeOrComplexTypeOrGroup
                        .filter { it is Element2 }.map { it as Element2 }.associateBy { it.name!! }),
                collectedInformation.complexTypes.plus(xsdItems.simpleTypeOrComplexTypeOrGroup
                        .filter { it is ComplexType }.map { it as ComplexType }.associateBy { it.name!! }))

        xsdItems.includeOrImportOrRedefine.forEach {
            parseXsdFileAndReferencesToOtherFiles(File(xsdFile.parentFile, (it as Include).schemaLocation), updatedMetadata)
        }

        return updatedMetadata
    }

    private fun representElement(element: Element2,
                                 metadata: CollectedInformation): Element {

        val elementName = element.name!!.localPart

        if (element.type != null) {
            val complexType = metadata.getComplexType(element.type)
            val children = extractChildElementsFromComplexType(complexType, metadata)
            return Element(elementName, children)
        } else if (element.complexType != null) {
            val complexType = element.complexType
            val children = extractChildElementsFromComplexType(complexType, metadata)
            return Element(elementName, children)
        }

        return Element(elementName)
    }

    private fun extractChildElementsFromComplexType(complexType: ComplexType?,
                                                    metadata: CollectedInformation): List<Element> {
        val subElements = complexType?.sequence?.particle ?: return listOf()
        return subElements
                .filter { it is LocalElement }
                .map { it as LocalElement }
                .map { it -> representElement(it, metadata) }
    }
}