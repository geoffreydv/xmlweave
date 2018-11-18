package com.xmlweave.element_representation

import java.io.File
import java.util.*
import javax.xml.namespace.QName

interface XsdStructureService {

    fun getElementStructure(xsdFile: File, elementToRender: QName?): Optional<Element>

}