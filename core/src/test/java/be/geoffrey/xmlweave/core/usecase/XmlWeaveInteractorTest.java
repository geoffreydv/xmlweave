package be.geoffrey.xmlweave.core.usecase;

import org.junit.Test;

import java.io.File;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlWeaveInteractorTest {

    @Test
    public void chooseInitialElementWithNoOptionsShouldNotReturnAnything() {

        File testFile = TestFileReader.readTestFile("2_simple_elements.xsd");

        XmlWeaveInteractor sut = new XmlWeaveInteractor();
        Optional<Element> representation = sut.getRepresentation(testFile, "");
        assertThat(representation).isNotPresent();
    }

    @Test
    public void chooseInitialElementWithSelectionShouldRepresentRootElement() {

        File testFile = TestFileReader.readTestFile("2_simple_elements.xsd");

        XmlWeaveInteractor sut = new XmlWeaveInteractor();
        Optional<Element> representation = sut.getRepresentation(testFile, "SimpleBasicElement");

        assertThat(representation).isPresent();
        assertThat(representation.get().getName()).isEqualTo("SimpleBasicElement");
    }

    @Test
    public void chooseInitialElementWithNonExistingSelectionShouldReturnNothing() {

        File testFile = TestFileReader.readTestFile("2_simple_elements.xsd");

        XmlWeaveInteractor sut = new XmlWeaveInteractor();
        Optional<Element> representation = sut.getRepresentation(testFile, "Bla");

        assertThat(representation).isNotPresent();
    }

    @Test
    public void simpleChildElementsShouldBeRenderedCorrectly() {

        File testFile = TestFileReader.readTestFile("1_simple_element_with_basic_childs.xsd");

        XmlWeaveInteractor sut = new XmlWeaveInteractor();
        Optional<Element> rep = sut.getRepresentation(testFile, "SimpleBasicElement");

        Element element = rep.get();

        assertThat(element.getChildren()).hasSize(2);
        assertThat(element.getChildren()
                .stream()
                .map(Element::getName)
                .collect(Collectors.toList()))
                .containsExactly("elementOne", "elementTwo");
    }

}