package be.geoffrey.xmlweave.core.usecase;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlWeaveInteractorTest {

    @Test
    public void chooseInitialElementShouldFindPossibilities() {

        File testFile = TestFileReader.readTestFile("2_simple_elements.xsd");

        XmlWeaveInteractor sut = new XmlWeaveInteractor();
        List<String> elements = sut.getPossibleElements(testFile);

        assertThat(elements).containsExactly("SimpleBasicElement", "AnotherSimpleBasicElement");

    }

}