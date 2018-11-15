package be.geoffrey.xmlweave.core.usecase.xmlrendering

import be.geoffrey.xmlweave.core.usecase.Element
import org.springframework.stereotype.Service

@Service
class XmlRenderer {
    fun renderAsXml(el: Element): String {

        if (el.children.isEmpty()) {
            return "<${el.name} />"
        } else {
            var rendered = "<${el.name}>\n"
            for (child in el.children) {
                rendered += " ".repeat(4) + renderAsXml(child) + "\n"
            }
            rendered += "</${el.name}>"
            return rendered.trimMargin()
        }
    }
}
