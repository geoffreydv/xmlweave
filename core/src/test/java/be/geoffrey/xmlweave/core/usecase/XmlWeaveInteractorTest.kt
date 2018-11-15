package be.geoffrey.xmlweave.core.usecase

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

        assertThat(rep.children
                .map { it.name })
                .containsExactly("elementOne", "elementTwo")
    }

    @Test
    fun simpleComplexTypeShouldBeInterpreted() {
        val rep = interpretTestFile("3_simple_element_with_non_local_complex_type.xsd", "SimpleBasicElement").get()

        assertThat(rep.children
                .map { it.name })
                .containsExactly("elementOne", "elementTwo")
    }

    private fun interpretTestFile(fileName: String, rootElement: String): Optional<Element> {
        val testFile = TestFileReader.readTestFile(fileName)
        val sut = XmlWeaveInteractor()
        return sut.getElementStructure(testFile, rootElement)
    }

}