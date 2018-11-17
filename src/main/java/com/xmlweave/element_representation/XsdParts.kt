package com.xmlweave.element_representation

import com.sun.org.apache.xerces.internal.dom.DeferredAttrImpl
import org.springframework.stereotype.Service
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory

class Schema(private val simpleTypeOrComplexTypeOrGroup: List<OpenAttrs> = listOf(),
             val namespace: String? = null,
             val nsReferences: HashMap<String, String>? = hashMapOf()) {

    fun getElement(name: String): TopLevelElement? {
        return simpleTypeOrComplexTypeOrGroup
                .filter { it is TopLevelElement }
                .map { it as TopLevelElement }
                .firstOrNull { it.name == name }
    }

    fun getComplexType(name: String): TopLevelComplexType? {
        return simpleTypeOrComplexTypeOrGroup
                .filter { it is TopLevelComplexType }
                .map { it as TopLevelComplexType }
                .firstOrNull { it.name == name }
    }
}

abstract class Group(val particle: List<Any> = listOf())

class ExplicitGroup(particle: List<Any> = listOf()) : Group(particle)

open class OpenAttrs

abstract class ComplexType(val name: String? = null,
                           val sequence: ExplicitGroup? = null) : OpenAttrs()

class TopLevelComplexType(name: String? = null,
                          sequence: ExplicitGroup? = null) : ComplexType(name, sequence)

abstract class Element2(val name: String? = null,
                        val type: QName? = null,
                        val complexType: ComplexType? = null) : OpenAttrs()

class TopLevelElement(name: String? = null,
                      type: QName? = null,
                      complexType: ComplexType? = null) : Element2(name, type, complexType)

class LocalElement(elementName: String? = null,
                   type: QName? = null,
                   complexType: ComplexType? = null) : Element2(elementName, type, complexType)

@Service
class XsdFile {
    fun parse(xsdFile: File): Schema {
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docBuilderFactory.newDocumentBuilder()
        val document = docBuilder.parse(xsdFile)
        return parseSchema(document.documentElement)
    }

    private fun parseSchema(element: Element): Schema {

        val allTopLevelTypes = ArrayList<OpenAttrs>()

        var namespace = ""
        val nsReferences = HashMap<String, String>()

        for (i in 0 until element.attributes.length) {
            val attr = element.attributes.item(i) as DeferredAttrImpl

            if (attr.name == "xmlns") {
                namespace = attr.value
            } else if (attr.name.startsWith("xmlns")) {
                val ns = attr.name.split(":").last()
                nsReferences[ns] = attr.value
            }
        }

        val topLevelElements = childrenByTag(element, "element")
                .map { parseElementDefinition(it, true) }

        val topLevelComplexTypes = childrenByTag(element, "complexType")
                .map { parseComplexType(it) }

        allTopLevelTypes.addAll(topLevelElements)
        allTopLevelTypes.addAll(topLevelComplexTypes)

        return Schema(allTopLevelTypes, namespace, nsReferences)
    }

    private fun parseElementDefinition(e: Element, top: Boolean = false): Element2 {

        val elementName = e.getAttribute("name")
        val ct = childByTag(e, "complexType")
        val typeAttribute = e.getAttribute("type")
        val type = if (typeAttribute != null && typeAttribute.isNotBlank()) interpretName(typeAttribute) else null
        val complexType = if (ct != null) parseComplexType(ct) else null

        if (top) {
            return TopLevelElement(elementName, type, complexType = complexType)
        }

        return LocalElement(elementName, type, complexType = complexType)
    }

    private fun parseComplexType(ct: Element): ComplexType {
        val sequence = childByTag(ct, "sequence")
        val elements = childrenByTag(sequence!!, "element")
                .map { parseElementDefinition(it) }
        val name = ct.getAttribute("name")
        return TopLevelComplexType(name, sequence = ExplicitGroup(elements))
    }

    private fun interpretName(name: String): QName {

        if (name.contains(":")) {
            val (prefix, localPart) = name.split(":")

            return QName("", localPart, prefix)
        }

        return QName("", name)
    }

    private fun childByTag(element: Element, tagName: String): Element? {
        val children = childrenByTag(element, tagName)
        if (!children.isEmpty()) {
            return children.first()
        }

        return null
    }

    private fun childrenByTag(element: Element, tagName: String): List<Element> {

        val children = element.childNodes

        val elements = java.util.ArrayList<Element>()
        for (i in 0 until children.length) {
            val child = children.item(i)

            if (child.nodeType == Node.ELEMENT_NODE) {
                val name = interpretName((child as Element).nodeName)
                if (name.localPart == tagName) {
                    elements.add(child)
                }
            }
        }
        return elements
    }
}