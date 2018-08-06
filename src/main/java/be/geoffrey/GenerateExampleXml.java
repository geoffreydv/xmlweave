package be.geoffrey;

import be.geoffrey.schemaparsing.NameAndNamespace;
import be.geoffrey.schemaparsing.SchemaFinder;
import be.geoffrey.schemaparsing.SchemaParser;
import be.geoffrey.schemaparsing.SchemaParsingContext;
import be.geoffrey.schemaparsing.grouping.XmlElement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class GenerateExampleXml {

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, TransformerException {

        // ---------- Milestone 1: Generate a completely valid XML for a given XSD element
        // TODO: Add configuration to enable switching for choice tags
        // TODO: Add minOccurs / maxOccurs support but just with 1 level, to enable mandatory / not mandatory
        // TODO: Add minOccurs / maxOccurs support, multi elements / path rendering with [0] etc
        // TODO: Add idea of lists etc to path traversal
        // TODO: Add types for attributes, now everything is a string
        // TODO: Add configuration to enable switching for minOccurs
        // TODO: Choices about minOccurs / maxOccurs etc :)
        // TODO: Allow adding defaults for type "Always choose x" when deciding
        // TODO: Make "reference" classes instead of NameAndNamespace (not clear)
        // TODO: Allow using "ref"
        // TODO: XS:ANY support
        // TODO: Add 'Default mode', right now it's 'fill as much as possible'
        // TODO: Same types get loaded multiple times I think (saw 2 breaks when parsing complexType 'GeneriekeOpdrachtType'
        // TODO: Add caching metadata to speed up generation
        // TODO: Allow imports without schemaLocation to define it manually
        // TODO: Come up with a good config file format
        // TODO: Afhandelen "default values" van types
        // TODO: Use "Optional<T>" like a baws
        // TODO: Validations etc, same class defined twice bvb
        //     - TODO: 2x hetzelfde geïmporteerd maar onder andere NS of whatever
        // TODO: MaxLength checks etc :)
        // TODO: Do an ultimate test where every file is validated against appropriate XSD with XMLStarlet
        // TODO: Look into auto regex generation
        // TODO: FOR wsdl compares, be able to do a fake render, and return a list of every complexType that this element uses
        // TODO: Load documentation metadata / annotations etc from xsd
        // ---------- Milestone einde: Throw every XSD against this thing to find issues

        String xsdPath = args[0];
        String elementName = args[1];
        String resultFile = args[2];
        String propertiesFilePath = args[3];

        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(propertiesFilePath)));

        SchemaFinder sf = new SchemaFinder();
        sf.build(xsdPath);

        SchemaParser parser = new SchemaParser();

        for (String schemaPath : sf.getFoundSchemas()) {
            parser.parseSchema(new File(schemaPath));
        }

        SchemaParsingContext results = parser.getResults();

        if (results != null) {
            String resultXml = generateXml(results, null, elementName, properties);
            FileUtils.writeStringToFile(new File(resultFile), resultXml, StandardCharsets.UTF_8);
        }
    }

    private static String generateXml(SchemaParsingContext context,
                                      String namespaceToSearch,
                                      String elementName,
                                      Properties decisionProperties) throws TransformerException, ParserConfigurationException {

        XmlElement element;

        if (StringUtils.isNotBlank(namespaceToSearch)) {
            NameAndNamespace reference = new NameAndNamespace(elementName, namespaceToSearch);
            element = context.getKnownElement(reference);
        } else {
            element = context.getKnownElementByElementName(elementName);
        }

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // root elements
        Element rootElement = element.render(doc, context, null, decisionProperties, true);

        doc.appendChild(rootElement);

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(doc);
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);

        transformer.transform(source, result);
        return sw.toString();
    }
}