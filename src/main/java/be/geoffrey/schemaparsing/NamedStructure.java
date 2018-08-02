package be.geoffrey.schemaparsing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public class NamedStructure implements StructureOfClass {

    private String namespace;
    private String name;

    // Attributes of Complex types =======================================

    private boolean abstractType;
    private NameAndNamespace extensionOf;

    // Used when this is a complex type
    private List<ElementType> elements = new ArrayList<>();

    // Attributes of simple types =======================================

    private NameAndNamespace basedOnBasicType;
    private List<String> possibleEnumValues = new ArrayList<>();
    private String basedOnRegex;

    public NamedStructure(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public List<ElementType> getElements() {
        return elements;
    }

    public NameAndNamespace getBasedOnBasicType() {
        return basedOnBasicType;
    }

    public NameAndNamespace getExtensionOf() {
        return extensionOf;
    }

    public void setBasedOnRegex(String regex) {
        this.basedOnRegex = regex;
    }

    public void setExtensionOf(NameAndNamespace extensionOf) {
        this.extensionOf = extensionOf;
    }

    public void setBasedOnBasicType(NameAndNamespace basedOnBasicType) {
        this.basedOnBasicType = basedOnBasicType;
    }

    public boolean isAbstractType() {
        return abstractType;
    }

    public void setAbstractType(boolean abstractType) {
        this.abstractType = abstractType;
    }

    public void addEnumValue(String value) {
        possibleEnumValues.add(value);
    }

    public void setPossibleEnumValues(List<String> possibleValues) {
        this.possibleEnumValues = possibleValues;
    }

    public boolean isExtensionOfOtherBaseType() {
        return extensionOf != null;
    }

    public String identity() {
        return namespace + "/" + name;
    }

    public Node asXmlTagWithName(String nameToUse, Document doc, SchemaParsingContext context) {

        if (abstractType) {
            throw new IllegalArgumentException("Trying to create an element of an abstract type...");
        } else if (basedOnBasicType != null) {
            return BasicTypeUtil.createBasicTypeElementWithNameAndValue(new NameAndNamespace(nameToUse, namespace), basedOnBasicType, doc, possibleEnumValues, basedOnRegex);
        } else {
            Element elementOfType = doc.createElementNS(namespace, nameToUse);
            for (ElementType element : elements) {
                Node childElement = element.toXmlNodeWithName(element.getName(), doc, context);
                elementOfType.appendChild(childElement);
            }
            return elementOfType;
        }
    }

    public void addElement(ElementType type) {
        elements.add(type);
    }

    public void addAllElementsAtBeginning(List<ElementType> elements) {
        this.elements.addAll(0, elements);
    }

    public NameAndNamespace reference() {
        return new NameAndNamespace(name, namespace);
    }
}
