package be.geoffrey.schemaparsing;

import be.geoffrey.fun.XmlElementRenderer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class GifroyTest {

    @Test
    public void simpleElementShouldRenderCorrectly() {

        File file = TestFileReader.readTestFile("xsd_files/1_simple.xsd");
        String output = new XmlElementRenderer(file, new HashMap<>()).renderElement("SimpleBasicElement");

        assertThat(output).isEqualTo("<SimpleBasicElement />");
    }

    @Test
    public void basicComplexTypeShouldRenderCorrectly() {

        File file = TestFileReader.readTestFile("xsd_files/2_complex_type_with_basic.xsd");
        String output = new XmlElementRenderer(file, new HashMap<>()).renderElement("SimpleBasicElement");

        assertThat(output).isEqualTo("<SimpleBasicElement></SimpleBasicElement>");
    }

}
