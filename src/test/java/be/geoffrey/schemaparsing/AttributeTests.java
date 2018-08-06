package be.geoffrey.schemaparsing;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: Add test cases with nesting and inheritance
public class AttributeTests {

    @Test
    public void testParseSingleLevelItems() throws IOException, ParserConfigurationException, SAXException {

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("attribute_super_test.xsd").getFile());

        SchemaParser parser = new SchemaParser();
        parser.parseSchema(file);
        SchemaParsingContext ctx = parser.getResults();

        NamedStructure ts = ctx.getKnownNamedStructureByName("AttributeTest");

        assertThat(ts.getAttributes()).extracting("name").contains("baseAttribute1", "my-attr");
    }
}
