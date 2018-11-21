package com.xmlweave.core.interpret_schema

import com.sun.org.apache.xerces.internal.dom.DeferredAttrImpl
import org.springframework.stereotype.Service
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory

class XsdFile(val simpleTypeOrComplexTypeOrGroup: List<OpenAttrs> = listOf(),
              val includeOrImportOrRedefine: List<OpenAttrs> = listOf(),
              val nsReferences: HashMap<String, String>? = hashMapOf())

class Import(val namespace: String, val schemaLocation: String) : OpenAttrs()

class Include(val schemaLocation: String) : OpenAttrs()

abstract class Group(val particle: List<Any> = listOf())

class ExplicitGroup(particle: List<Any> = listOf()) : Group(particle)

open class OpenAttrs

abstract class SimpleType

abstract class ComplexType(val name: QName? = null,
                           val sequence: ExplicitGroup? = null) : OpenAttrs()

class TopLevelComplexType(name: QName? = null,
                          sequence: ExplicitGroup? = null) : ComplexType(name, sequence)

class LocalComplexType(name: QName? = null,
                       sequence: ExplicitGroup? = null) : ComplexType(name, sequence)

abstract class Element2(val name: QName? = null,
                        val type: QName? = null,
                        val complexType: ComplexType? = null) : OpenAttrs()

class TopLevelElement(name: QName? = null,
                      type: QName? = null,
                      complexType: ComplexType? = null) : Element2(name, type, complexType)

class LocalElement(name: QName? = null,
                   type: QName? = null,
                   complexType: ComplexType? = null) : Element2(name, type, complexType)

object XsdFileParser {

    fun parseSingleXsd(xsdFile: File): XsdFile {
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docBuilderFactory.newDocumentBuilder()
        val document = docBuilder.parse(xsdFile)
        return parseSchema(document.documentElement)
    }

    private fun parseSchema(element: Element): XsdFile {

        val nsReferences = HashMap<String, String>()

        for (i in 0 until element.attributes.length) {
            val attr = element.attributes.item(i) as DeferredAttrImpl

            if (attr.name.startsWith("xmlns")) {
                if (attr.name.contains(":")) {
                    val ns = attr.name.split(":").last()
                    nsReferences[ns] = attr.value
                } else {
                    nsReferences[""] = attr.value
                }
            }
        }

        val referencesToOtherSchemas = childrenByTagIgnoreNs(element, "import").map {
            Import(it.getAttribute("namespace"), it.getAttribute("schemaLocation"))
        }.plus(childrenByTagIgnoreNs(element, "include").map {
            Include(it.getAttribute("schemaLocation"))
        })

        val allTopLevelTypes = ArrayList<OpenAttrs>()
        allTopLevelTypes.addAll(childrenByTagIgnoreNs(element, "element").map { parseElementDefinition(it, nsReferences, true) })
        allTopLevelTypes.addAll(childrenByTagIgnoreNs(element, "complexType").map { parseComplexType(it, nsReferences, top = true) })

        return XsdFile(allTopLevelTypes, referencesToOtherSchemas, nsReferences)
    }

    private fun parseElementDefinition(e: Element, nsRef: HashMap<String, String>, top: Boolean = false): Element2 {

        val elementName = e.getAttribute("name")
        val ct = childByTag(e, "complexType")
        val typeAttribute = e.getAttribute("type")
        val type = if (typeAttribute != null && typeAttribute.isNotBlank()) qname(typeAttribute, nsRef) else null
        val complexType = if (ct != null) parseComplexType(ct, nsRef, true) else null

        if (top) {
            return TopLevelElement(QName(nsRef[""], elementName), type, complexType)
        }

        return LocalElement(QName(nsRef[""], elementName), type, complexType)
    }

    private fun parseComplexType(ct: Element, nsRef: HashMap<String, String>, top: Boolean): ComplexType {
        val sequence = childByTag(ct, "sequence")
        val elements = childrenByTagIgnoreNs(sequence!!, "element")
                .map { parseElementDefinition(it, nsRef) }
        val name = ct.getAttribute("name")

        return if (top) {
            TopLevelComplexType(QName(nsRef[""], name), ExplicitGroup(elements))
        } else {
            LocalComplexType(QName(nsRef[""], name), ExplicitGroup(elements))
        }
    }

    private fun qname(name: String,
                      nsRef: HashMap<String, String>): QName {

        if (name.contains(":")) {
            val (prefix, localPart) = name.split(":")

            return QName(nsRef[prefix], localPart)
        }

        return QName(nsRef[""], name)
    }

    private fun childByTag(element: Element, tagName: String): Element? {
        val children = childrenByTagIgnoreNs(element, tagName)
        if (!children.isEmpty()) {
            return children.first()
        }

        return null
    }

    private fun childrenByTagIgnoreNs(element: Element, tagName: String): List<Element> {

        val elements = mutableListOf<Element>()

        val children = element.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)

            if (child.nodeType == Node.ELEMENT_NODE) {
                if ((child as Element).nodeName.endsWith(tagName)) {
                    elements.add(child)
                }
            }
        }
        return elements
    }
}