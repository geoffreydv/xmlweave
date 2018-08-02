package be.geoffrey.schemaparsing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public class NamedStructure implements StructureOfClass {

    private String namespace;
    private String name;

    private boolean abstractType;
    private NameAndNamespace extensionOf;

    // Used when this is a complex type
    private List<ElementType> elements = new ArrayList<>();

    // Used when this is a simple type
    private NameAndNamespace simpleTypeBase;
    // When it is an 'enum'
    private List<String> possibleEnumValues = new ArrayList<>();
    // When it is based on a regex pattern
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

    public NameAndNamespace getSimpleTypeBase() {
        return simpleTypeBase;
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

    public void setSimpleTypeBase(NameAndNamespace simpleTypeBase) {
        this.simpleTypeBase = simpleTypeBase;
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

    public boolean isExtensionOfOtherBaseType() {
        return extensionOf != null;
    }

    public String identity() {
        return namespace + "/" + name;
    }

    public Node asXmlTagWithName(String nameToUse, Document doc, SchemaParsingContext context) {

        if (abstractType) {
            throw new IllegalArgumentException("Trying to create an element of an abstract type...");
        } else if (simpleTypeBase != null) {
            return BasicTypeUtil.createBasicTypeElementWithNameAndValue(new NameAndNamespace(nameToUse, namespace), simpleTypeBase, doc, possibleEnumValues, basedOnRegex);
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
