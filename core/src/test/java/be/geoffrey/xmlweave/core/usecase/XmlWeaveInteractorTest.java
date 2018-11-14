package be.geoffrey.xmlweave.core.usecase;

import be.geoffrey.xmlweave.core.usecase.choice.ListChoice;
import be.geoffrey.xmlweave.core.usecase.choice.Representation;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlWeaveInteractorTest {

    @Test
    public void chooseInitialElementShouldFindPossibilities() {

        File testFile = TestFileReader.readTestFile("2_simple_elements.xsd");

        XmlWeaveInteractor sut = new XmlWeaveInteractor();
//        Representation choice = sut.getRepresentation(testFile, new ArrayList<>());

//        assertThat(choice.getRootElement()).isNull();
//        assertThat(choice.getPossibleChoices().get("/")).isInstanceOf(ListChoice.class);
//        assertThat(((ListChoice) choice.getPossibleChoices().get("/")).getPossibleChoices())
//                .contains("SimpleBasicElement", "AnotherSimpleBasicElement");
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
//            assertThat(subChoice).isExactlyInstanceOf(Element.class);
//        }
//        assertThat(choice.getSubChoices())
//                .extracting("describe")
//                .containsExactly("elementOne", "elementTwo");
//    }

}