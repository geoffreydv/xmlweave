package be.geoffrey.xmlweave.core.usecase;

import org.junit.Test;

import java.io.File;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlWeaveInteractorTest {

    @Test
    public void chooseInitialElementWithNoOptionsShouldNotReturnAnything() {

        File testFile = TestFileReader.readTestFile("2_simple_elements.xsd");

        XmlWeaveInteractor sut = new XmlWeaveInteractor();
        Optional<ElementRepresentation> representation = sut.getRepresentation(testFile, "");
        assertThat(representation).isNotPresent();
    }

    @Test
    public void chooseInitialElementWithSelectionShouldRepresentRootElement() {

        File testFile = TestFileReader.readTestFile("2_simple_elements.xsd");

        XmlWeaveInteractor sut = new XmlWeaveInteractor();
        Optional<ElementRepresentation> representation = sut.getRepresentation(testFile, "SimpleBasicElement");

        assertThat(representation).isPresent();
        assertThat(representation.get().getName()).isEqualTo("SimpleBasicElement");
    }

    @Test
    public void chooseInitialElementWithNonExistingSelectionShouldReturnNothing() {

        File testFile = TestFileReader.readTestFile("2_simple_elements.xsd");

        XmlWeaveInteractor sut = new XmlWeaveInteractor();
        Optional<ElementRepresentation> representation = sut.getRepresentation(testFile, "Bla");

        assertThat(representation).isNotPresent();
    }

//    @Test
//    public void getStructureWithBasicChildElementsShouldReturnCorrectly() {
//
//        File testFile = TestFileReader.readTestFile("1_simple_element_with_basic_childs.xsd");
//
//        XmlWeaveInteractor sut = new XmlWeaveInteractor();
//        List<Decision> decisions = new ArrayList<>();
//        decisions.add(new RootElementDecision("SimpleBasicElement"));
//
//        Representation choice = sut.getRepresentation(testFile, decisions);
//
//        assertThat(choice.getSubChoices()).hasSize(2);
//        for (Representation subChoice : choice.getSubChoices()) {
//            assertThat(subChoice).isExactlyInstanceOf(ElementRepresentation.class);
//        }
//        assertThat(choice.getSubChoices())
//                .extracting("describe")
//                .containsExactly("elementOne", "elementTwo");
//    }

}