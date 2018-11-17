import com.xmlweave.element_representation.Element
import com.xmlweave.xmlrendering.XmlRenderer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class XmlRenderingTest {

    @Test
    fun simpleElementShouldBeRendered() {
        val renderer = XmlRenderer()
        assertThat(renderer.renderAsXml(Element("Abc"))).isEqualTo("<Abc/>")
        assertThat(renderer.renderAsXml(Element("Element"))).isEqualTo("<Element/>")
    }

    @Test
    fun simpleElementWithChildrenShouldBeRendered() {
        val renderer = XmlRenderer()
        val testElement = Element("Hello",
                children = listOf(
                        Element("World"),
                        Element("Wooooo"))
        )

        assertThat(renderer.renderAsXml(testElement)).isEqualTo(trimmed(
                """
                <Hello>
                    <World/>
                    <Wooooo/>
                </Hello>
                """))
    }

    @Test
    fun multiLevelIndentationShouldRenderFine() {
        val renderer = XmlRenderer()
        val testElement = Element("Hello", children = listOf(
                Element("World", listOf(
                        Element("Test")))
        ))

        assertThat(renderer.renderAsXml(testElement)).isEqualTo(trimmed(
                """
                <Hello>
                    <World>
                        <Test/>
                    </World>
                </Hello>
                """))
    }

    @Test
    fun testPrefixRendering() {
        assertThat(XmlRenderer().renderAsXml(Element("Hoi", prefix = "root")))
                .isEqualTo(trimmed("""
                    <root:Hoi/>
                    """))

        assertThat(XmlRenderer().renderAsXml(Element("Hoi", prefix = "root", children = listOf(Element("placeholder")))))
                .isEqualTo(trimmed("""
                    <root:Hoi>
                        <placeholder/>
                    </root:Hoi>
                    """))
    }

    private fun trimmed(input: String): String {
        return input.trimIndent()
    }

}
