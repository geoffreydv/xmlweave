package be.geoffrey.schemaparsing;

import be.geoffrey.schemaparsing.grouping.Choice;
import be.geoffrey.schemaparsing.grouping.Sequence;
import be.geoffrey.schemaparsing.grouping.StructurePart;
import be.geoffrey.schemaparsing.grouping.XmlElement;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class SchemaParser {

    private static final String SCHEMA_NS = "SCHEMA_NS";

    private SchemaParsingContext context;

    public SchemaParser() {
        this.context = null;
    }

    public void parseSchema(File schemaFile) throws ParserConfigurationException, IOException, SAXException {
        this.context = parseSchema(schemaFile, null, this.context);
    }

    private SchemaParsingContext parseSchema(File schemaFile, String schemaNamespaceOverride,
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

    public SchemaParsingContext getResults() {
        while (context.needsInheritanceEnhancement()) {
            context.addAllDataOfBaseClassesToConcreteImplementations();
        }

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

            // TODO: This is valid, the element has no default xs type, handle this properly
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

        Element elementDefinitionWrapper = findXmlElementThatCanContainElementDefinitions(complexType, knownNamespaces);

        if (elementDefinitionWrapper != null) {
            List<StructurePart> collectedStructureParts = parseStructurePartsInWrapper(elementDefinitionWrapper, knownNamespaces, context);
            namedStructure.addStructurePartsAtBeginning(collectedStructureParts);
        } else {
            System.out.println("WARNING: No fields were found for type " + namedStructure.getName() + ", better check if this is correct :) " + context.getFileName());
        }

        // TODO: Continue here...
        Element attributeWrapper = findXmlElementThatCanContainAttributeDefinitions(complexType, knownNamespaces);
        if (attributeWrapper != null) {
            List<XmlAttribute> collectedAttributes = parseDirectChildAttributesOfWrapper(attributeWrapper, knownNamespaces);
            namedStructure.addAllAttributesAtBeginning(collectedAttributes);
        }
    }

    private static Element findXmlElementThatCanContainAttributeDefinitions(Element complexType,
                                                                            Map<String, String> knownNamespaces) {

        return selectFirstOccurrenceOfAny(complexType, Lists.newArrayList(
                "simpleContent.extension", "complexContent.extension", ""
        ), knownNamespaces);
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

    private static List<StructurePart> parseStructurePartsInWrapper(Element wrappingTag,
                                                                    Map<String, String> knownNamespaces,
                                                                    SchemaParsingContext context) {

        List<StructurePart> parts = new ArrayList<>();

        NodeList childNodes = wrappingTag.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node cn = childNodes.item(i);

            if (cn.getNodeType() == Node.ELEMENT_NODE) {
                NameAndNamespace nn = parseReference(cn.getNodeName(), knownNamespaces);

                switch (nn.getName()) {
                    case "element":
                        parts.add(extractInfoFromXmlElement(knownNamespaces, context, (Element) cn));
                        break;
                    case "sequence":
                        List<StructurePart> childrenOfSequence = parseStructurePartsInWrapper((Element) cn, knownNamespaces, context);
                        parts.add(new Sequence(childrenOfSequence));
                        break;
                    case "choice":
                        List<StructurePart> childrenOfChoice = parseStructurePartsInWrapper((Element) cn, knownNamespaces, context);
                        parts.add(new Choice(childrenOfChoice));
                        break;
                }
            }
        }

        return parts;
    }

    private static XmlElement extractInfoFromXmlElement(Map<String, String> knownNamespaces, SchemaParsingContext context, Element element) {
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
    }


    private static Element findXmlElementThatCanContainElementDefinitions(Element complexType, Map<String, String> knownNamespaces) {

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
