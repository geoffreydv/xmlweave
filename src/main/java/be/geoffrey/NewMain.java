package be.geoffrey;

import be.geoffrey.schemaparsing.*;
import com.google.common.collect.Lists;
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

        // TODO: XS:Choice support
        // TODO: Support switching concrete classes with implementations
        // TODO: Better attribute parsing, also make sure all elements that define attributes are found
        // TODO: Choices about minOccurs / maxOccurs etc :)
        // TODO: Add regex configuration support to provide a default etc
        // TODO: Allow adding defaults for type "Always choose x" when deciding
        // TODO: Add idea of lists etc to path traversal
        // TODO: Add support for ATTRIBUTES as well :D
        // TODO: Make "reference" classes instead of NameAndNamespace (not clear)
        // TODO: XS:ANY support
        // TODO: Same types get loaded multiple times I think (saw 2 breaks when parsing complexType 'GeneriekeOpdrachtType'
        // TODO: Add caching metadata to speed up generation
        // TODO: Correct namespace handling (define at top)
        // TODO: Allow imports without schemaLocation to define it manually
        // TODO: Come up with a good config file format
        // TODO: Afhandelen "default values" van types
        // TODO: Write some utility methods to find tags bla.bla2.bla3 that returns tag
        // TODO: Make a class out of the searching etc, with context as a private field...
        // TODO: Use "Optional<T>" like a baws
        // TODO: Validations etc, same class defined twice bvb
        //     - TODO: 2x hetzelfde geïmporteerd maar onder andere NS of whatever
        // TODO: Load documentation metadata / annotations etc from xsd
        // TODO: Look into auto regex generation
        // TODO: FOR wsdl compares, be able to do a fake render, and return a list of every complexType that this element uses

        String xsdPath = args[0];
        String elementName = args[1];
        String resultFile = args[2];

        SchemaFinder sf = new SchemaFinder();
        sf.build(xsdPath);

        SchemaParsingContext context = null;

        for (String schemaPath : sf.getFoundSchemas()) {
            context = parseDirectChildrenOfSchema(new File(schemaPath), null, context);

            while (context.needsInheritanceEnhancement()) {
                context.addAllDataOfBaseClassesToConcreteImplementations();
            }
        }

        if (context != null) {
            String resultXml = generateXml(context, null, elementName);
            FileUtils.writeStringToFile(new File(resultFile), resultXml, StandardCharsets.UTF_8);
        }
    }

    private static String generateXml(SchemaParsingContext context, String ns, String elementName) throws TransformerException, ParserConfigurationException {

        XmlElement element;

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
        Element rootElement = element.render(doc, context, null);
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

    private static SchemaParsingContext parseDirectChildrenOfSchema(File schemaFile, String schemaNamespaceOverride,
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

                        XmlElement thisElement = parseElementDefinition(childAsElement, knownNamespaces, context);
                        context.addKnownRootElement(thisElement);
                        break;

                    case "complexType": {
                        NamedStructure thisType = parseRootComplexType(knownNamespaces, childAsElement, context);

                        if (thisType.isExtensionOfOtherCustomType()) {
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
                            context.addInfoFromOtherSchema(parseDirectChildrenOfSchema(includedFileReference, null, context));
                        }
                        break;
                    }
                    case "import": {
                        String schemaLocation = childAsElement.getAttribute("schemaLocation");
                        String overrideNs = childAsElement.getAttribute("namespace");
                        File includedFileReference = new File(schemaFile.getParentFile(), schemaLocation);

                        if (!context.isSchemaAlreadyParsed(includedFileReference.getAbsolutePath())) {
                            context.addInfoFromOtherSchema(parseDirectChildrenOfSchema(includedFileReference, overrideNs, context));
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

    private static NamedStructure parseRootComplexType(Map<String, String> knownNamespaces,
                                                       Element complexType,
                                                       SchemaParsingContext context) {

        String ns = knownNamespaces.get(SCHEMA_NS);
        String nameOfComplexType = complexType.getAttribute("name");

        NamedStructure namedStructure = new NamedStructure(ns, nameOfComplexType);
        fillNamedStructureWithInformationFromXmlComplexType(namedStructure, complexType, knownNamespaces, context);

        return namedStructure;
    }

    private static NamedStructure parseRootSimpleType(Element simpleTypeElement,
                                                      Map<String, String> knownNamespaces) {

        String ns = knownNamespaces.get(SCHEMA_NS);
        String name = simpleTypeElement.getAttribute("name");
        NamedStructure namedStructure = new NamedStructure(ns, name);

        fillNamedStructureWithInformationFromXmlSimpleType(namedStructure, simpleTypeElement, knownNamespaces);

        return namedStructure;
    }


    private static XmlElement parseElementDefinition(Element element, Map<String, String> knownNamespaces,
                                                     SchemaParsingContext context) {

        String namespaceOfCurrentSchema = knownNamespaces.get(SCHEMA_NS);
        String nameOfThisElement = element.getAttribute("name");

        if (element.hasAttribute("type")) {
            return new XmlElement(namespaceOfCurrentSchema, nameOfThisElement, parseReference(element.getAttribute("type"), knownNamespaces));
        }

        // This element can also define its own fields, without using a type reference.
        // In this case we create a placeholder type and assign that to the element

        NamedStructure dynamicType = createDynamicNamedStructure(knownNamespaces, nameOfThisElement);

        Element complexType = childByTag(element, "complexType", knownNamespaces);
        Element simpleType = childByTag(element, "simpleType", knownNamespaces);

        // Add the complex content fields to the custom type
        if (complexType != null) {
            fillNamedStructureWithInformationFromXmlComplexType(dynamicType, complexType, knownNamespaces, context);
        } else if (simpleType != null) {
            fillNamedStructureWithInformationFromXmlSimpleType(dynamicType, simpleType, knownNamespaces);
        } else {
            throw new IllegalArgumentException("An element contains a structure definition that is not known yet.");
        }

        // Add this type to the context
        context.addKnownNamedStructure(dynamicType);

        // Assign this custom named structure as the type of the element
        return new XmlElement(namespaceOfCurrentSchema, nameOfThisElement, dynamicType.reference());
    }

    private static void fillNamedStructureWithInformationFromXmlComplexType(NamedStructure namedStructure,
                                                                            Element complexType,
                                                                            Map<String, String> knownNamespaces,
                                                                            SchemaParsingContext context) {

        namedStructure.setAbstractType("true".equals(complexType.getAttribute("abstract")));
        NameAndNamespace parentClass = findBaseClassThisTypeIsExtending(complexType, knownNamespaces);

        if (parentClass != null) {
            namedStructure.setExtensionOf(parentClass);
        }

        Element wrappingElement = findXmlElementThatCanWrapElements(complexType, knownNamespaces);

        if (wrappingElement != null) {
            List<XmlElement> collectedElements = parseDirectChildElementsOfWrapper(wrappingElement, knownNamespaces, context);

            // TODO: Split in 2 cases: elements and attributes
            // TODO: Voorlopig is dit: find classes that can wrap ELEMENTS
            List<XmlAttribute> collectedAttributes = parseDirectChildAttributesOfWrapper(wrappingElement, knownNamespaces);

            namedStructure.addAllElementsAtBeginning(collectedElements);
            namedStructure.addAllAttributesAtBeginning(collectedAttributes);
        } else {
            System.out.println("WARNING: No fields were found for type " + namedStructure.getName() + ", better check if this is correct :) " + context.getFileName());
        }
    }

    private static NameAndNamespace findBaseClassThisTypeIsExtending(Element complexType,
                                                                     Map<String, String> knownNamespaces) {

        Element elementThatWrapsExtension = childByTag(complexType, "complexContent", knownNamespaces);

        if (elementThatWrapsExtension == null) {
            elementThatWrapsExtension = childByTag(complexType, "simpleContent", knownNamespaces);
        }

        if (elementThatWrapsExtension == null) {
            return null;
        }

        Element extension = childByTag(elementThatWrapsExtension, "extension", knownNamespaces);

        if (extension == null) {
            return null;
        }

        return parseReference(extension.getAttribute("base"), knownNamespaces);
    }


    private static List<XmlAttribute> parseDirectChildAttributesOfWrapper(Element wrappingTag,
                                                                          Map<String, String> knownNamespaces) {

        List<Element> xmlAttributes = childrenWithTag(wrappingTag, "attribute", knownNamespaces);

        return xmlAttributes.stream()
                .map(element -> {
                    String typeOfAttribute = element.getAttribute("type");
                    NameAndNamespace typeRef = parseReference(typeOfAttribute, knownNamespaces);
                    return new XmlAttribute(element.getAttribute("name"), typeRef);
                })
                .collect(Collectors.toList());
    }

    private static List<XmlElement> parseDirectChildElementsOfWrapper(Element wrappingTag,
                                                                      Map<String, String> knownNamespaces,
                                                                      SchemaParsingContext context) {

        List<Element> xmlElements = childrenWithTag(wrappingTag, "element", knownNamespaces);

        return xmlElements.stream()
                .map(element -> {
                    String typeOfElement = element.getAttribute("type");

                    if (StringUtils.isNotBlank(typeOfElement)) {
                        // This is easy, the element has a type reference
                        String name = element.getAttribute("name");
                        XmlElement xmlElement = new XmlElement(knownNamespaces.get(SCHEMA_NS), name, parseReference(typeOfElement, knownNamespaces));
                        xmlElement.setMinOccurs(element.getAttribute("minOccurs"));
                        xmlElement.setMaxOccurs(element.getAttribute("maxOccurs"));
                        return xmlElement;
                    }

                    return parseElementDefinition(element, knownNamespaces, context);
                })
                .collect(Collectors.toList());
    }


    private static Element findXmlElementThatCanWrapElements(Element complexType, Map<String, String> knownNamespaces) {

        // TODO: At some point maybe I should just search down to find the first occurrence (might not always work)

        return selectFirstOccurrenceOfAny(complexType, Lists.newArrayList(
                "sequence",
                "complexContent.extension.sequence",
                "simpleContent.restriction.sequence",
                "simpleContent.extension.sequence",
                "choice",
                "complexContent.extension.choice",
                "simpleContent.restriction.choice",
                "simpleContent.extension.choice"
        ), knownNamespaces);
    }

    private static Element selectFirstOccurrenceOfAny(Element rootElement,
                                                      List<String> selectors,
                                                      Map<String, String> knownNamespaces) {

        for (String selector : selectors) {
            Element result = select(rootElement, selector, knownNamespaces);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static void fillNamedStructureWithInformationFromXmlSimpleType(NamedStructure namedStructure,
                                                                           Element simpleType,
                                                                           Map<String, String> knownNamespaces) {

        Element restriction = childByTag(simpleType, "restriction", knownNamespaces);

        if (restriction == null) {
            throw new IllegalArgumentException("I can't handle this yet...");
        }

        namedStructure.setBasedOnBasicType(parseReference(restriction.getAttribute("base"), knownNamespaces));

        // If it is an enum...
        List<Element> enumValues = childrenWithTag(restriction, "enumeration", knownNamespaces);
        if (enumValues.size() > 0) {
            for (Element enumValue : enumValues) {
                namedStructure.addEnumValue(enumValue.getAttribute("value"));
            }
        }
        // If it is based on a regex...
        Element pattern = childByTag(restriction, "pattern", knownNamespaces);
        if (pattern != null) {
            namedStructure.setBasedOnRegex(pattern.getAttribute("value"));
        }
    }

    private static NamedStructure createDynamicNamedStructure(Map<String, String> knownNamespaces,
                                                              String nameOfThisElement) {

        return new NamedStructure(knownNamespaces.get(SCHEMA_NS),
                String.format("%s_%s", UUID.randomUUID().toString(), nameOfThisElement));
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

    private static Element select(Element base, String selection, Map<String, String> knownNamespaces) {

        String[] parts = selection.split("\\.");

        Element cursor = base;

        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                Element child = childByTag(cursor, part, knownNamespaces);

                if (child == null) {
                    return null;
                }

                cursor = child;
            }
        }

        return cursor;
    }
}
