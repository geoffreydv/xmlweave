package be.geoffrey.schemaparsing;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class NestingTests {

    @Test
    public void testParseSingleLevelItems() throws IOException, ParserConfigurationException, SAXException {

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("multi_level_test.xsd").getFile());

        SchemaParsingContext ctx = SchemaParser.parseDirectChildrenOfSchema(file);

        assertThat(ctx.getKnownNamedStructures().values())
                .extracting("baseName")
                .contains("a", "d", "BaseElement");

        System.out.println("HEY");
    }
}
