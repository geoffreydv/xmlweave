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

        // TODO: Better Parsing
        // TODO: Support switching concrete classes with implementation
        // TODO: Make "reference" classes instead of NameAndNamespace (not clear)
        // TODO: Prevent stackoverflows
        // TODO: XS:ANY support
        // TODO: XS:Choice support
        // TODO: Think about "Element", it shouldn't be a different thing, only a reference to a type
        // TODO: Add caching metadata to speed up generation
        // TODO: Correct namespace handling (define at top)
        // TODO: Afhandelen "default values" van types

        String xsdPath = args[0];
        String elementName = args[1];
        String resultFile = args[2];

        SchemaFinder sf = new SchemaFinder();
        sf.build(xsdPath);

        SchemaParsingContext context = null;

        for (String schemaPath : sf.getFoundSchemas()) {
            context = parseRootElementsOfSchema(new File(schemaPath), null, context);

            while (context.needsInheritanceEnhancement()) {
                context.addAllFieldsOfBaseClassesToConcreteImplementations();
            }
        }

        if (context != null) {
            String resultXml = generateXml(context, null, elementName);
            FileUtils.writeStringToFile(new File(resultFile), resultXml, StandardCharsets.UTF_8);
        }
    }

    private static String generateXml(SchemaParsingContext context, String ns, String elementName) throws TransformerException, ParserConfigurationException {

        RootElement element;

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
        Node rootElement = element.asXmlTag(doc, context);
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

    private static SchemaParsingContext parseRootElementsOfSchema(File schemaFile, String schemaNamespaceOverride,
                                                                  SchemaParsingContext previousMetadata) throws ParserConfigurationException, IOException, SAXException {

        SchemaParsingContext context = new SchemaParsingContext(schemaFile.getAbsolutePath(), previousMetadata);

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

                NameAndNamespace nns = parseReference(childAsElement.getNodeName(), knownNamespaces);

                switch (nns.getName()) {
                    case "element":

                        RootElement thisElement = parseRootElementDefinition(knownNamespaces, childAsElement, context);
                        context.addKnownRootElement(thisElement);
                        break;

                    case "complexType": {
                        NamedStructure thisType = parseRootComplexType(knownNamespaces, childAsElement, context);

                        if (thisType.isExtensionOfOtherBaseType()) {
                            context.indicateElementRequiresInheritanceEnhancement(thisType);
                        }

                        context.addKnownNamedStructure(thisType);
                        break;
                    }
                    case "simpleType": {
                        NamedStructure thisType = parseRootSimpleType(childAsElement, knownNamespaces);
                        context.addKnownNamedStructure(thisType);
                        break;
                    }
                    case "include": {
                        String schemaLocation = childAsElement.getAttribute("schemaLocation");
                        File includedFileReference = new File(schemaFile.getParentFile(), schemaLocation);

                        if (!context.isSchemaAlreadyParsed(includedFileReference.getAbsolutePath())) {
                            context.addInfoFromOtherSchema(parseRootElementsOfSchema(includedFileReference, null, context));
                        }
                        break;
                    }
                    case "import": {
                        String schemaLocation = childAsElement.getAttribute("schemaLocation");
                        String overrideNs = childAsElement.getAttribute("namespace");
                        File includedFileReference = new File(schemaFile.getParentFile(), schemaLocation);

                        if (!context.isSchemaAlreadyParsed(includedFileReference.getAbsolutePath())) {
                            context.addInfoFromOtherSchema(parseRootElementsOfSchema(includedFileReference, overrideNs, context));
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

    private static NamedStructure parseRootSimpleType(Element simpleTypeElement,
                                                      Map<String, String> knownNamespaces) {

        return parseRootSimpleType(simpleTypeElement, simpleTypeElement.getAttribute("name"), knownNamespaces);
    }

    private static NamedStructure parseRootSimpleType(Element simpleTypeElement,
                                                      String nameOverride,
                                                      Map<String, String> knownNamespaces) {

        String ns = knownNamespaces.get(SCHEMA_NS);

        if (StringUtils.isBlank(nameOverride)) {
            throw new IllegalArgumentException("Simple type has no name, please define one.");
        }

        NamedStructure thisType = new NamedStructure(ns, nameOverride);
        Element restriction = childByTag(simpleTypeElement, "restriction", knownNamespaces);
        if (restriction != null) {
            thisType.setSimpleTypeBase(parseReference(restriction.getAttribute("base"), knownNamespaces));

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

    private static RootElement parseRootElementDefinition(Map<String, String> knownNamespaces,
                                                          Element element,
                                                          SchemaParsingContext context) {

        String namespaceOfCurrentSchema = knownNamespaces.get(SCHEMA_NS);
        String nameOfThisElement = element.getAttribute("name");

        // TODO: Het enige verschil tussen deze en de volgende is dat het geen naam heeft

        if (element.hasAttribute("type")) {
            return new RootElement(namespaceOfCurrentSchema, nameOfThisElement, parseReference(element.getAttribute("type"), knownNamespaces));
        }

        // This element can also define its own fields, without using a type reference.
        // In this case we create a fake bunch of fields and assign that as the type

        NamedStructure dynamicType = createDynamicNamedStructure(element, knownNamespaces, context, nameOfThisElement);

        // Add this type to the context
        context.addKnownNamedStructure(dynamicType);

        // Save this element as an element that has the custom class as its type
        return new RootElement(namespaceOfCurrentSchema, nameOfThisElement, dynamicType.reference());
    }

    private static NamedStructure createDynamicNamedStructure(Element elementThatDefinesAStructure,
                                                              Map<String, String> knownNamespaces,
                                                              SchemaParsingContext context,
                                                              String nameOfThisElement) {

        NamedStructure dynamicClass = new NamedStructure(knownNamespaces.get(SCHEMA_NS), UUID.randomUUID().toString() + "_" + nameOfThisElement);



        List<ElementType> elements = loadElementsDefinedInType(elementThatDefinesAStructure, knownNamespaces, context);

        for (ElementType elementType : elements) {
            dynamicClass.addElement(elementType);
        }

        return dynamicClass;
    }

    private static List<ElementType> loadElementsDefinedInType(Element xmlElementContainingStructure,
                                                               Map<String, String> knownNamespaces,
                                                               SchemaParsingContext context) {

        Element sequenceTag = findSequenceTagInsideXmlElement(knownNamespaces, xmlElementContainingStructure);

        if (sequenceTag == null) {
            return new ArrayList<>();
        }

        return loadElementsFromSequenceOfElements(knownNamespaces, sequenceTag, context);
    }

    private static NamedStructure parseRootComplexType(Map<String, String> knownNamespaces,
                                                       Element complexType,
                                                       SchemaParsingContext context) {

        String ns = knownNamespaces.get(SCHEMA_NS);
        String nameOfComplexType = complexType.getAttribute("name");

        NamedStructure namedStructure = new NamedStructure(ns, nameOfComplexType);
        namedStructure.setAbstractType("true".equals(complexType.getAttribute("abstract"))); // Dit is specifiek aan complexContent dat als root gedefiniëerd staat

        // Add known elements
        List<ElementType> elements = loadElementsDefinedInType(complexType, knownNamespaces, context);

        for (ElementType element : elements) {
            namedStructure.addElement(element);
        }

        // Collect data about inheritance
        Element complexContent = childByTag(complexType, "complexContent", knownNamespaces);
        if (complexContent != null) {
            Element extension = childByTag(complexContent, "extension", knownNamespaces);
            if (extension != null) {
                NameAndNamespace baseClass = parseReference(extension.getAttribute("base"), knownNamespaces);
                namedStructure.setExtensionOf(baseClass);
            }
        }

        return namedStructure;
    }

    private static Element findSequenceTagInsideXmlElement(Map<String, String> knownNamespaces, Element xmlElementContainingStructure) {

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
                            NamedStructure st = parseRootSimpleType(simpleType, randomNameForThisElement, knownNamespaces);
                            context.addKnownNamedStructure(st);

                            return new ElementType(name, minOccurs, new NameAndNamespace(st.getName(), st.getNamespace()));
                        }
                    }

                    return new ElementType(name, minOccurs, parseReference(typeOfElement, knownNamespaces));
                })
                .collect(Collectors.toList());
    }

    private static List<Element> childrenWithTag(Element root, String tagName, Map<String, String> knownNamespaces) {
        NodeList children = root.getChildNodes();

        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                NameAndNamespace nns = parseReference(child.getNodeName(), knownNamespaces);
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

    private static NameAndNamespace parseReference(String name, Map<String, String> knownNamespaces) {
        if (name.contains(":")) {
            String[] parts = name.split(":");
            return new NameAndNamespace(parts[1], knownNamespaces.get(parts[0]));
        } else {
            return new NameAndNamespace(name, knownNamespaces.get(SCHEMA_NS));
        }
    }
}
