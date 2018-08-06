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

        SchemaParser parser = new SchemaParser();
        parser.parseSchema(file);
        SchemaParsingContext ctx = parser.getResults();

        assertThat(ctx.getKnownNamedStructures().values())
                .extracting("baseName")
                .contains("a", "d", "BaseElement");
    }

    @Test
    public void testParseIfrs9Items() throws IOException, SAXException, ParserConfigurationException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("multi_level_ifrs9.xsd").getFile());

        SchemaParser parser = new SchemaParser();
        parser.parseSchema(file);
        SchemaParsingContext ctx = parser.getResults();

        assertThat(ctx.getKnownNamedStructures().values())
                .extracting("baseName")
                .contains("financialInstruments", "parties", "collaterals", "dataSet");
    }
}
