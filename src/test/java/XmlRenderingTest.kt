import com.xmlweave.element_representation.Element
import com.xmlweave.xmlrendering.XmlRenderer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class XmlRenderingTest {

    @Test
    fun simpleElementShouldBeRendered() {
        val renderer = XmlRenderer()
        assertThat(renderer.renderAsXml(Element("Abc", prefix = "root"))).isEqualTo("<Abc/>")
        assertThat(renderer.renderAsXml(Element("Element", prefix = "root"))).isEqualTo("<Element/>")
    }

    @Test
    fun simpleElementWithChildrenShouldBeRendered() {
        val renderer = XmlRenderer()
        val testElement = Element("Hello",
                children = listOf(
                        Element("World", prefix = "root"),
                        Element("Wooooo", prefix = "root")),
                prefix = "root")

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
                        Element("Test", prefix = "root")), prefix = "root")
        ), prefix = "root")

        assertThat(renderer.renderAsXml(testElement)).isEqualTo(trimmed(
                """
                <Hello>
                    <World>
                        <Test/>
                    </World>
                </Hello>
                """))
    }

    private fun trimmed(input: String): String {
        return input.trimIndent()
    }

}
