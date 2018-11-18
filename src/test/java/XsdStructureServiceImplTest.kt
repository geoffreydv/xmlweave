import com.xmlweave.element_representation.Attribute
import com.xmlweave.element_representation.Element
import com.xmlweave.element_representation.XsdFileParser
import com.xmlweave.element_representation.XsdStructureServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*
import javax.xml.namespace.QName

class XsdStructureServiceImplTest {

    @Test
    fun chooseInitialElementWithNoOptionsShouldNotReturnAnything() {
        val representation = interpretTestFile("2_simple_elements.xsd", null)
        assertThat(representation).isNotPresent
    }

    @Test
    fun chooseInitialElementWithSelectionShouldRepresentRootElement() {
        val representation = interpretTestFile("2_simple_elements.xsd", QName("SimpleBasicElement"))
        assertThat(representation).isPresent
        assertThat(representation.get().name).isEqualTo("SimpleBasicElement")
    }

    @Test
    fun chooseInitialElementWithNonExistingSelectionShouldReturnNothing() {
        val representation = interpretTestFile("2_simple_elements.xsd", QName("Bla"))
        assertThat(representation).isNotPresent
    }

    @Test
    fun simpleChildElementsShouldBeInterpretedCorrectly() {
        val rep = interpretTestFile("1_simple_element_with_local_complex_type.xsd", QName("SimpleBasicElement")).get()
        assertThat(rep).isEqualTo(Element("SimpleBasicElement",
                listOf(Element("elementOne"), Element("elementTwo"))))
    }

    @Test
    fun simpleNonLocalComplexTypeShouldBeInterpreted() {
        val rep = interpretTestFile("3_simple_element_with_non_local_complex_type.xsd", QName("SimpleBasicElement")).get()
        assertThat(rep).isEqualTo(Element("SimpleBasicElement",
                listOf(Element("elementOne"), Element("elementTwo"))))
    }

    @Test
    fun simpleElementWithInnerComplexType() {
        val rep = interpretTestFile("4_simple_element_with_local_complex_type_in_child_element.xsd", QName("SimpleBasicElement")).get()

        assertThat(rep).isEqualTo(Element("SimpleBasicElement",
                listOf(Element("Child",
                        listOf(Element("elementOne"), Element("elementTwo")))
                )))
    }

    @Test
    fun rootElementShouldGetTargetNamespaceOfSchemaIfItHasOne() {
        val rep = interpretTestFile("5_namespace_test.xsd", QName("test-namespace", "SimpleBasicElement")).get()

        assertThat(rep).isEqualTo(Element("SimpleBasicElement",
                prefix = "root",
                attributes = listOf(Attribute("xmlns:root", "test-namespace"))))
    }

    @Test
    fun testIncludeRemoteComplexType() {
        val rep = interpretTestFile("6_include_stuff_no_prefix.xsd", QName("SimpleBasicElement")).get()
        assertThat(rep).isEqualTo(Element("SimpleBasicElement",
                listOf(Element("elementOne"), Element("elementTwo"))))
    }

    private fun interpretTestFile(fileName: String, rootElement: QName?): Optional<Element> {
        val testFile = TestFileReader.readTestFile(fileName)
        val sut = XsdStructureServiceImpl(XsdFileParser())
        return sut.getElementStructure(testFile, rootElement)
    }
}