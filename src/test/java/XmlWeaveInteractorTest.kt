import com.xmlweave.element_representation.Element
import com.xmlweave.element_representation.XmlWeaveInteractor
import com.xmlweave.element_representation.SchemaParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class XmlWeaveInteractorTest {

    @Test
    fun chooseInitialElementWithNoOptionsShouldNotReturnAnything() {
        val representation = interpretTestFile("2_simple_elements.xsd", "")
        assertThat(representation).isNotPresent
    }

    @Test
    fun chooseInitialElementWithSelectionShouldRepresentRootElement() {
        val representation = interpretTestFile("2_simple_elements.xsd", "SimpleBasicElement")
        assertThat(representation).isPresent
        assertThat(representation.get().name).isEqualTo("SimpleBasicElement")
    }

    @Test
    fun chooseInitialElementWithNonExistingSelectionShouldReturnNothing() {
        val representation = interpretTestFile("2_simple_elements.xsd", "Bla")
        assertThat(representation).isNotPresent
    }

    @Test
    fun simpleChildElementsShouldBeInterpretedCorrectly() {
        val rep = interpretTestFile("1_simple_element_with_local_complex_type.xsd", "SimpleBasicElement").get()
        assertThat(rep).isEqualTo(Element("SimpleBasicElement",
                listOf(Element("elementOne"), Element("elementTwo"))))
    }

    @Test
    fun simpleNonLocalComplexTypeShouldBeInterpreted() {
        val rep = interpretTestFile("3_simple_element_with_non_local_complex_type.xsd", "SimpleBasicElement").get()
        assertThat(rep).isEqualTo(Element("SimpleBasicElement",
                listOf(Element("elementOne"), Element("elementTwo"))))
    }

    @Test
    fun simpleElementWithInnerComplexType() {
        val rep = interpretTestFile("4_simple_element_with_local_complex_type_in_child_element.xsd", "SimpleBasicElement").get()

        assertThat(rep).isEqualTo(Element("SimpleBasicElement",
                listOf(Element("Child",
                        listOf(Element("elementOne"), Element("elementTwo")))
                )))
    }

    private fun interpretTestFile(fileName: String, rootElement: String): Optional<Element> {
        val testFile = TestFileReader.readTestFile(fileName)
        val sut = XmlWeaveInteractor(SchemaParser())
        return sut.getElementStructure(testFile, rootElement)
    }
}