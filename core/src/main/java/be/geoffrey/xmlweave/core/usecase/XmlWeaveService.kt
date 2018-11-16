package be.geoffrey.xmlweave.core.usecase

import be.geoffrey.xmlweave.core.usecase.element_representation.Element
import java.io.File
import java.util.*

interface XmlWeaveService {

    fun getElementStructure(xsdFile: File, elementName: String?): Optional<Element>

    fun renderElementAsXml(e: Element): String

}