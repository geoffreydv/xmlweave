package be.geoffrey.xmlweave.core.usecase

import java.io.File
import java.util.*

interface XmlWeaveService {
    fun getRepresentation(xsdFile: File, rootName: String?): Optional<Element>
}