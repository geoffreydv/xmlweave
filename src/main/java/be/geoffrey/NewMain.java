package be.geoffrey;

import be.geoffrey.schemaparsing.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class NewMain {

    private static final String SCHEMA_NS = "SCHEMA_NS";

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, TransformerException {

        // TODO: Support switching concrete classes with implementation
        // TODO: Prevent stackoverflows
        // TODO: XS:ANY support
        // TODO: XS:Choice support
        // TODO: Think about "Element", it shouldn't be a different thing, only a reference to a type
        // TODO: Add caching metadata to speed up generation

        String xsdPath = args[0];
        String elementName = args[1];
        String resultFile = args[2];

        SchemaFinder sf = new SchemaFinder();
        sf.build(xsdPath);

        SchemaParsingContext context = null;

        for (String schemaPath : sf.getFoundSchemas()) {
            context = parseSchema(new File(schemaPath), null, context);

            while(context.needsInheritanceEnhancement()) {
                context.addAllFieldsOfBaseClassesToConcreteImplementations();
            }

        }

        if (context != null) {
            String resultXml = generateXml(context, null, elementName);
            FileUtils.writeStringToFile(new File(resultFile), resultXml, StandardCharsets.UTF_8);
        }
    }

    private static String generateXml(SchemaParsingContext context, String ns, String elementName) throws TransformerException, ParserConfigurationException {

        KnownElement element;

        if (StringUtils.isNotBlank(ns)) {
            NameAndNamespace reference = new NameAndNamespace(elementName, ns);
            element = context.getKnownElement(reference);
        } else {
            element = context.getKnownElementByElementName(elementName);
        }

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

        transformer.transform(source, result);
        return sw.toString();
    }

    private static SchemaParsingContext parseSchema(File schemaFile, String schemaNamespaceOverride,
                                                    SchemaParsingContext previouslyCollectedMetadata) throws ParserConfigurationException, IOException, SAXException {

        SchemaParsingContext context = new SchemaParsingContext(schemaFile.getAbsolutePath(), previouslyCollectedMetadata);
//        System.out.println("Parsing schema: " + context.getFileName());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(context.getFileName());

        Element schema = document.getDocumentElement();
        Map<String, String> knownNamespaces = collectNamespacesDefinedInSchema(schemaNamespaceOverride, schema);

        NodeList children = schema.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childAsElement = (Element) child;

                NameAndNamespace nns = parse(childAsElement.getNodeName(), knownNamespaces);

                switch (nns.getName()) {
                    case "element":
                        KnownElement thisElement = parseKnownElement(knownNamespaces, childAsElement, context);
                        context.addKnownElement(thisElement);
                    case "complexType": {
                        KnownXmlType thisType = parseKnownComplexType(knownNamespaces, childAsElement, context);

                        if (thisType.isExtensionOfOtherBaseType()) {
                            context.indicateElementRequiresInheritanceEnhancement(thisType);
                        }

                        context.addKnownXmlType(thisType);
                        break;
                    }
                    case "simpleType": {
                        KnownXmlType thisType = parseKnownSimpleType(childAsElement, knownNamespaces);
                        context.addKnownXmlType(thisType);
                        break;
                    }
                    case "include": {
                        String schemaLocation = childAsElement.getAttribute("schemaLocation");
                        File includedFileReference = new File(schemaFile.getParentFile(), schemaLocation);

                        if (!context.isSchemaAlreadyParsed(includedFileReference.getAbsolutePath())) {
                            context.addInfoFromOtherSchema(parseSchema(includedFileReference, null, context));
                        }
                        break;
                    }
                    case "import": {
                        String schemaLocation = childAsElement.getAttribute("schemaLocation");
                        String overrideNs = childAsElement.getAttribute("namespace");
                        File includedFileReference = new File(schemaFile.getParentFile(), schemaLocation);

                        if (!context.isSchemaAlreadyParsed(includedFileReference.getAbsolutePath())) {
                            context.addInfoFromOtherSchema(parseSchema(includedFileReference, overrideNs, context));
                        }

                        break;
                    }
                }
            }
        }

        context.indicateFileParsingComplete();
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

    private static KnownXmlType parseKnownSimpleType(Element simpleTypeElement, Map<String, String> knownNamespaces) {
        return parseKnownSimpleType(simpleTypeElement, simpleTypeElement.getAttribute("name"), knownNamespaces);
    }

    private static KnownXmlType parseKnownSimpleType(Element simpleTypeElement, String nameOverride, Map<String, String> knownNamespaces) {

        String ns = knownNamespaces.get(SCHEMA_NS);

        if (StringUtils.isBlank(nameOverride)) {
            throw new IllegalArgumentException("Simple type has no name, please define one.");
        }

        KnownXmlType thisType = new KnownXmlType(ns, nameOverride);
        Element restriction = childByTag(simpleTypeElement, "restriction", knownNamespaces);
        if (restriction != null) {
            thisType.setSimpleTypeBase(parse(restriction.getAttribute("base"), knownNamespaces));

            // If it is an enum...
            List<Element> enumValues = childrenWithTag(restriction, "enumeration", knownNamespaces);
            if (enumValues.size() > 0) {
                for (Element enumValue : enumValues) {
                    thisType.addEnumValue(enumValue.getAttribute("value"));
                }
            }
            // If it is based on a regex...
            Element pattern = childByTag(restriction, "pattern", knownNamespaces);
            if (pattern != null) {
                thisType.setBasedOnRegex(pattern.getAttribute("value"));
            }
        }
        return thisType;
    }

    private static KnownXmlType parseKnownComplexType(Map<String, String> knownNamespaces, Element complexType, SchemaParsingContext context) {

        // TODO: typename zou wel eens een schema kunnen bevatten, check

        String ns = knownNamespaces.get(SCHEMA_NS);
        String complexTypeName = complexType.getAttribute("name");
        KnownXmlType thisType = new KnownXmlType(ns, complexTypeName);
        thisType.setAbstractType("true".equals(complexType.getAttribute("abstract")));

        // Add known elements
        List<ElementType> elements = extractXmlElements(knownNamespaces, complexType, context);

        for (ElementType element : elements) {
            thisType.addElement(element);
        }

        // Collect data about inheritance
        Element complexContent = childByTag(complexType, "complexContent", knownNamespaces);
        if (complexContent != null) {
            Element extension = childByTag(complexContent, "extension", knownNamespaces);
            if (extension != null) {
                NameAndNamespace baseClass = parse(extension.getAttribute("base"), knownNamespaces);
                thisType.setExtensionOf(baseClass);
            }
        }

        return thisType;
    }

    private static KnownElement parseKnownElement(Map<String, String> knownNamespaces, Element element, SchemaParsingContext context) {

        KnownElement thisType = new KnownElement(knownNamespaces.get(SCHEMA_NS), element.getAttribute("name"));

        List<ElementType> elements = extractXmlElements(knownNamespaces, element, context);

        for (ElementType elementType : elements) {
            thisType.addElement(elementType);
        }

        return thisType;
    }

    private static List<ElementType> extractXmlElements(Map<String, String> knownNamespaces,
                                                        Element xmlElementContainingStructure,
                                                        SchemaParsingContext context) {

        Element sequenceTag = findSequenceInXmlElement(knownNamespaces, xmlElementContainingStructure);

        if (sequenceTag == null) {
            return new ArrayList<>();
        }

        return loadElementsFromSequenceOfElements(knownNamespaces, sequenceTag, context);
    }

    private static Element findSequenceInXmlElement(Map<String, String> knownNamespaces, Element xmlElementContainingStructure) {

        // TODO: Als dit te gek wordt misschien een recursief zoekding maken

        Element sequenceTag = childByTag(xmlElementContainingStructure, "sequence", knownNamespaces);

        if (sequenceTag != null) {
            return sequenceTag;
        } else {
            Element complexType = childByTag(xmlElementContainingStructure, "complexType", knownNamespaces);
            if (complexType != null) {
                Element sequence = childByTag(complexType, "sequence", knownNamespaces);
                if (sequence != null) {
                    return sequence;
                }
            } else {
                Element complexContent = childByTag(xmlElementContainingStructure, "complexContent", knownNamespaces);
                if (complexContent != null) {
                    Element extensionElement = childByTag(complexContent, "extension", knownNamespaces);
                    if (extensionElement != null) {
                        Element sequence = childByTag(extensionElement, "sequence", knownNamespaces);
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
                                                                        Element sequenceTag,
                                                                        SchemaParsingContext context) {

        // TODO: Dit moet eigenlijk een reeks items teruggeven, ipv eentje, die worden dan allemaal toegevoegd in de context
        // TODO: Try later
        // TODO: Dit omdat meerdere nieuwe elementen kunnen gedefiniëerd worden in 1 complex type

        List<Element> xmlElements = childrenWithTag(sequenceTag, "element", knownNamespaces);

        return xmlElements.stream()
                .map(element -> {
                    String name = element.getAttribute("name");
                    String minOccurs = element.getAttribute("minOccurs");
                    String typeOfElement = element.getAttribute("type");

                    // TODO: complexType kan ook ...
                    if (StringUtils.isBlank(typeOfElement)) {

                        Element simpleType = childByTag(element, "simpleType", knownNamespaces);
                        if (simpleType != null) {
                            // Definitie wordt hier verder uitgewerkt...

                            String randomNameForThisElement = UUID.randomUUID().toString() + "_" + name;
                            KnownXmlType st = parseKnownSimpleType(simpleType, randomNameForThisElement, knownNamespaces);
                            context.addKnownXmlType(st);

                            return new ElementType(name, minOccurs, new NameAndNamespace(st.getName(), st.getNamespace()));
                        }
                    }

                    return new ElementType(name, minOccurs, parse(typeOfElement, knownNamespaces));
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

    private static Element childByTag(Element root, String tagName, Map<String, String> knownNamespaces) {
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
