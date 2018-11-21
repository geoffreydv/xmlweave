import com.xmlweave.core.Attribute
import com.xmlweave.core.Element
import com.xmlweave.core.XmlRenderer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class XmlRenderingTest {

    @Test
    fun simpleElementShouldBeRendered() {
        assertThat(XmlRenderer.renderAsXml(Element("Abc"))).isEqualTo("<Abc/>")
        assertThat(XmlRenderer.renderAsXml(Element("Element"))).isEqualTo("<Element/>")
    }

    @Test
    fun simpleElementWithChildrenShouldBeRendered() {
        val testElement = Element("Hello",
                children = listOf(
                        Element("World"),
                        Element("Wooooo"))
        )

        assertThat(XmlRenderer.renderAsXml(testElement)).isEqualTo(trimmed(
                """
                <Hello>
                    <World/>
                    <Wooooo/>
                </Hello>
                """))
    }

    @Test
    fun multiLevelIndentationShouldRenderFine() {
        val testElement = Element("Hello", children = listOf(
                Element("World", listOf(
                        Element("Test")))
        ))

        assertThat(XmlRenderer.renderAsXml(testElement)).isEqualTo(trimmed(
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
        assertThat(XmlRenderer.renderAsXml(Element("Hoi", prefix = "root")))
                .isEqualTo(trimmed("""
                    <root:Hoi/>
                    """))

        assertThat(XmlRenderer.renderAsXml(Element("Hoi", prefix = "root", children = listOf(Element("placeholder")))))
                .isEqualTo(trimmed("""
                    <root:Hoi>
                        <placeholder/>
                    </root:Hoi>
                    """))
    }

    @Test
    fun testAttributeRenderingCases() {

        // Given
        val el = Element("Hoi", attributes = listOf(Attribute("a:b", "woot")))

        // When
        val output = XmlRenderer.renderAsXml(el)

        // Then
        assertThat(output).isEqualTo(trimmed("""
                    <Hoi a:b="woot"/>
                    """))
    }

    private fun trimmed(input: String): String {
        return input.trimIndent()
    }
}