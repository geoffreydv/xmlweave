package be.geoffrey.schemaparsing;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

// TODO: Add test cases with nesting and inheritance
public class SingleLevelTests {

    @Test
    public void testParseSingleLevelItems() throws IOException, ParserConfigurationException, SAXException {

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("single_level_tests.xsd").getFile());

        SchemaParsingContext ctx = SchemaParser.parseDirectChildrenOfSchema(file);

        assertThat(ctx.getKnownElements().values())
                .extracting("name", "structureReference.baseName")
                .containsExactly(tuple("elementWithDefinedComplexType", "ComplexType"),
                        tuple("elementWithDefinedSimpleType", "SimpleType"),
                        tuple("elementWithOwnComplexType", "elementWithOwnComplexType"),
                        tuple("elementWithOwnSimpleType", "elementWithOwnSimpleType"));

        assertThat(ctx.getKnownNamedStructures().values())
                .extracting("baseName")
                .contains(
                        "elementWithOwnComplexType",
                        "elementWithOwnSimpleType",
                        "ComplexType",
                        "SimpleType");

    }
}
