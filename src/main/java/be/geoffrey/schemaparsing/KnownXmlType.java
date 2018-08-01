package be.geoffrey.schemaparsing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public class KnownXmlType implements StructureOfClass {

    private String namespace;
    private String name;

    // Used when this is a complex type
    private List<ElementType> elements = new ArrayList<>();

    // Used when this is a simple type
    private NameAndNamespace simpleTypeBase;
    private List<String> possibleEnumValues = new ArrayList<>();

    public KnownXmlType(String namespace, String name) {
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

    public void setSimpleTypeBase(NameAndNamespace simpleTypeBase) {
        this.simpleTypeBase = simpleTypeBase;
    }

    public void addElement(ElementType type) {
        elements.add(type);
    }

    public void addEnumValue(String value) {
        possibleEnumValues.add(value);
    }

    public String identity() {
        return namespace + "/" + name;
    }

    public Node asXmlTagWithName(String nameToUse, Document doc, SchemaMetadata context) {
        if (simpleTypeBase != null) {
            return BasicTypeUtil.createBasicTypeElementWithNameAndValue(new NameAndNamespace(nameToUse, namespace), simpleTypeBase, doc, possibleEnumValues);
        } else {
            Element elementOfType = doc.createElementNS(namespace, nameToUse);
            for (ElementType element : elements) {
                elementOfType.appendChild(element.toXmlNodeWithName(element.getName(), doc, context));
            }
            return elementOfType;
        }
    }
}
