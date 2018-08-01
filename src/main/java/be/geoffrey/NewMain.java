package be.geoffrey;

import be.geoffrey.schemaparsing.*;
import org.w3c.dom.*;
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
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NewMain {

    private static final String SCHEMA_NS = "SCHEMA_NS";

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, TransformerException {

        File schema = new File("C:\\Users\\geoff\\Desktop\\edelta wsdl compare\\v16\\Aanbieden\\GeefOpdrachtDienst-05.00\\GeefOpdrachtWsResponse.xsd");

        SchemaMetadata context = parseSchema(schema, null);

        generateXml("http://webservice.geefopdrachtwsdienst-02_00.edelta.mow.vlaanderen.be", "GeefOpdrachtWsResponse", context);
    }

    private static void generateXml(String ns, String elementName, SchemaMetadata context) throws TransformerException, ParserConfigurationException {
        NameAndNamespace reference = new NameAndNamespace(elementName, ns);
        KnownElement element = context.getKnownElement(reference);

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // root elements
        Element rootElement = element.toXmlTag(doc, context);
        doc.appendChild(rootElement);

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(doc);
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);

        // Output to console for testing
        // StreamResult result = new StreamResult(System.out);

        transformer.transform(source, result);

        System.out.println(sw.toString());
    }

    private static SchemaMetadata parseSchema(File schemaFile, String schemaNamespaceOverride) throws ParserConfigurationException, IOException, SAXException {

        String normalizedFileName = Paths.get(schemaFile.getAbsolutePath()).normalize().toString();

        System.out.println("Parsing schema: " + normalizedFileName);

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(normalizedFileName);

        Element schema = document.getDocumentElement();

        SchemaMetadata context = new SchemaMetadata();

        Map<String, String> knownNamespaces = collectNamespacesDefinedInSchema(schemaNamespaceOverride, schema);

        NodeList children = schema.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childAsElement = (Element) child;

                NameAndNamespace nns = parse(childAsElement.getNodeName(), knownNamespaces);

                switch (nns.getName()) {
                    case "element":
                        KnownElement thisElement = parseKnownElement(knownNamespaces, childAsElement);
                        context.addKnownElement(thisElement);
                    case "complexType": {
                        KnownXmlType thisType = parseKnownComplexType(knownNamespaces, childAsElement);

                        if (thisType.isConcreteImplementationOfAbstract()) {
                            context.indicateElementRequiresInheritanceEnhancement(thisType);
                        }

                        context.addKnownXmlType(thisType);
                        break;
                    }
                    case "simpleType": {
                        KnownXmlType thisType = parseKnownSimpleType(knownNamespaces, childAsElement);
                        context.addKnownXmlType(thisType);
                        break;
                    }
                    case "include": {
                        String schemaLocation = childAsElement.getAttribute("schemaLocation");
                        File includedFileReference = new File(schemaFile.getParentFile(), schemaLocation);
                        context.addInfoFromOtherSchema(parseSchema(includedFileReference, null));
                        break;
                    }
                    case "import": {
                        String schemaLocation = childAsElement.getAttribute("schemaLocation");
                        String overrideNs = childAsElement.getAttribute("namespace");
                        File includedFileReference = new File(schemaFile.getParentFile(), schemaLocation);
                        context.addInfoFromOtherSchema(parseSchema(includedFileReference, overrideNs));
                        break;
                    }
                }
            }
        }

        context.addAllFieldsOfBaseClassesToConcreteImplementations();

        return context;
    }

    private static Map<String, String> collectNamespacesDefinedInSchema(String schemaNamespaceOverride, Element schema) {
        Map<String, String> knownNamespaces = new HashMap<>();
        knownNamespaces.put(SCHEMA_NS, schemaNamespaceOverride != null ? schemaNamespaceOverride : schema.getAttribute("xmlns"));

        NamedNodeMap schemaAttributes = schema.getAttributes();
        for (int i = 0; i < schemaAttributes.getLength(); i++) {
            Attr attr = (Attr) schemaAttributes.item(i);
            String attributeName = attr.getName();
            if (attributeName.startsWith("xmlns:")) {
                String nsName = attributeName.split(":")[1];
                String nsUri = attr.getValue();

                knownNamespaces.put(nsName, nsUri);
            }
        }
        return knownNamespaces;
    }

    private static KnownXmlType parseKnownSimpleType(Map<String, String> knownNamespaces, Element simpleTypeElement) {
        String ns = knownNamespaces.get(SCHEMA_NS);
        KnownXmlType thisType = new KnownXmlType(ns, simpleTypeElement.getAttribute("name"));
        Element restriction = childWithTag(simpleTypeElement, "restriction", knownNamespaces);
        if (restriction != null) {
            thisType.setSimpleTypeBase(parse(restriction.getAttribute("base"), knownNamespaces));

            List<Element> enumValues = childrenWithTag(restriction, "enumeration", knownNamespaces);
            if (enumValues.size() > 0) {
                for (Element enumValue : enumValues) {
                    thisType.addEnumValue(enumValue.getAttribute("value"));
                }
            }
        }
        return thisType;
    }

    private static KnownXmlType parseKnownComplexType(Map<String, String> knownNamespaces, Element complexType) {

        // TODO: typename zou wel eens een schema kunnen bevatten, check

        String ns = knownNamespaces.get(SCHEMA_NS);
        KnownXmlType thisType = new KnownXmlType(ns, complexType.getAttribute("name"));
        thisType.setAbstractType("true".equals(complexType.getAttribute("abstract")));

        // Add known elements
        List<ElementType> elements = extractXmlElements(knownNamespaces, complexType);

        for (ElementType element : elements) {
            thisType.addElement(element);
        }

        // Collect data about inheritance
        Element complexContent = childWithTag(complexType, "complexContent", knownNamespaces);
        if (complexContent != null) {
            Element extension = childWithTag(complexContent, "extension", knownNamespaces);
            if (extension != null) {
                NameAndNamespace baseClass = parse(extension.getAttribute("base"), knownNamespaces);
                thisType.setExtensionOf(baseClass);
            }
        }

        return thisType;
    }

    private static KnownElement parseKnownElement(Map<String, String> knownNamespaces, Element element) {

        KnownElement thisType = new KnownElement(knownNamespaces.get(SCHEMA_NS), element.getAttribute("name"));

        List<ElementType> elements = extractXmlElements(knownNamespaces, element);

        for (ElementType elementType : elements) {
            thisType.addElement(elementType);
        }

        return thisType;
    }

    private static List<ElementType> extractXmlElements(Map<String, String> knownNamespaces,
                                                        Element xmlElementContainingStructure) {

        Element sequenceTag = findSequenceInXmlElement(knownNamespaces, xmlElementContainingStructure);

        if (sequenceTag == null) {
            return new ArrayList<>();
        }

        return loadElementsFromSequenceOfElements(knownNamespaces, sequenceTag);
    }

    private static Element findSequenceInXmlElement(Map<String, String> knownNamespaces, Element xmlElementContainingStructure) {

        // TODO: Als dit te gek wordt misschien een recursief zoekding maken

        Element sequenceTag = childWithTag(xmlElementContainingStructure, "sequence", knownNamespaces);

        if (sequenceTag != null) {
            return sequenceTag;
        } else {
            Element complexType = childWithTag(xmlElementContainingStructure, "complexType", knownNamespaces);
            if (complexType != null) {
                Element sequence = childWithTag(complexType, "sequence", knownNamespaces);
                if (sequence != null) {
                    return sequence;
                }
            } else {
                Element complexContent = childWithTag(xmlElementContainingStructure, "complexContent", knownNamespaces);
                if (complexContent != null) {
                    Element extensionElement = childWithTag(complexContent, "extension", knownNamespaces);
                    if (extensionElement != null) {
                        Element sequence = childWithTag(extensionElement, "sequence", knownNamespaces);
                        if (sequence != null) {
                            return sequence;
                        }
                    }
                }
            }
        }
        return null;
    }


    private static List<ElementType> loadElementsFromSequenceOfElements(Map<String, String> knownNamespaces,
                                                                        Element sequenceTag) {

        List<Element> xmlElements = childrenWithTag(sequenceTag, "element", knownNamespaces);

        return xmlElements.stream()
                .map(element -> {
                    String name = element.getAttribute("name");
                    String minOccurs = element.getAttribute("minOccurs");
                    return new ElementType(name, minOccurs, parse(element.getAttribute("type"), knownNamespaces));
                })
                .collect(Collectors.toList());
    }

    private static List<Element> childrenWithTag(Element root, String tagName, Map<String, String> knownNamespaces) {
        NodeList children = root.getChildNodes();

        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                NameAndNamespace nns = parse(child.getNodeName(), knownNamespaces);
                if (nns.getName().equals(tagName)) {
                    elements.add((Element) child);
                }
            }
        }
        return elements;
    }

    private static Element childWithTag(Element root, String tagName, Map<String, String> knownNamespaces) {
        List<Element> children = childrenWithTag(root, tagName, knownNamespaces);

        if (children.isEmpty()) {
            return null;
        }

        return children.get(0);
    }

    private static NameAndNamespace parse(String name, Map<String, String> knownNamespaces) {
        if (name.contains(":")) {
            String[] parts = name.split(":");
            return new NameAndNamespace(parts[1], knownNamespaces.get(parts[0]));
        } else {
            return new NameAndNamespace(name, knownNamespaces.get(SCHEMA_NS));
        }
    }
}
