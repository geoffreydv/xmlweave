package com.xmlweave.ui

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Route
import com.xmlweave.core.element_representation.XsdStructureExtractor
import com.xmlweave.core.xmlrendering.XmlRenderer
import java.io.File
import javax.xml.namespace.QName

@Route("")
class GenerationView() : VerticalLayout() {

    init {
        buildUI()
    }

    private fun buildUI() {

        val fileField = TextField("File location:")
        fileField.value = "C:\\projects\\roots\\mowesb-fuse\\edelta-connector\\src\\main\\resources\\META-INF\\wsdl\\v18\\Aanbieden\\GeefOpdrachtDienst-05.00\\GeefOpdrachtWsResponse.xsd"
        fileField.setSizeFull()

        val generatedCode = TextArea()
        generatedCode.setSizeFull()
        generatedCode.isEnabled = false
        generatedCode.height = "600px"

        val rootElementBox = TextField("Root Element")
        rootElementBox.value = "GeefOpdrachtWsResponse"

        val button = Button("Generate XML")
        button.addClickListener {
            val representation = XsdStructureExtractor.getElementStructure(File(fileField.value), QName("http://webservice.geefopdrachtwsdienst-02_00.edelta.mow.vlaanderen.be", rootElementBox.value))

            if (representation.isPresent) {
                generatedCode.value = XmlRenderer.renderAsXml(representation.get())
            } else {
                generatedCode.value = "Nothing useful can be rendered."
            }
        }

        add(H1("XMLWeave"), fileField, rootElementBox, button, generatedCode)
    }
}
