package be.geoffrey.xmlweave.core.usecase

import be.geoffrey.xmlweave.core.usecase.xmlrendering.XmlRenderer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class XmlRenderingTest {

    @Test
    fun simpleElementShouldBeRendered() {
        val renderer = XmlRenderer()
        assertThat(renderer.renderAsXml(Element("Abc"))).isEqualTo("<Abc />")
        assertThat(renderer.renderAsXml(Element("Element"))).isEqualTo("<Element />")
    }

    @Test
    fun simpleElementWithChildrenShouldBeRendered() {
        val renderer = XmlRenderer()
        val testElement = Element("Hello",
                children = listOf(
                        Element("World"),
                        Element("Wooooo")))

        assertThat(renderer.renderAsXml(testElement)).isEqualTo(trimmed(
                """
                <Hello>
                    <World />
                    <Wooooo />
                </Hello>
                """))
    }

    private fun trimmed(input: String): String {
        return input.trimIndent()
    }

}
