package com.xmlweave.element_representation

import java.io.File
import java.util.*

interface XmlWeaveService {

    fun getElementStructure(xsdFile: File, elementName: String?): Optional<Element>

    fun renderElementAsXml(e: Element): String

}