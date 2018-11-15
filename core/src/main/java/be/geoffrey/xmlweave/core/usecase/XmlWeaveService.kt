package be.geoffrey.xmlweave.core.usecase

import java.io.File
import java.util.*

interface XmlWeaveService {

    fun getElementStructure(xsdFile: File, rootName: String?): Optional<Element>

    fun renderElementAsXml(e: Element): String

}