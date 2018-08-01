package be.geoffrey.schemaparsing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public class KnownXmlType implements StructureOfClass {

    private String namespace;
    private String name;

    private List<ElementType> elements = new ArrayList<>();

    private NameAndNamespace simpleTypeBase;

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

    public String identity() {
        return namespace + "/" + name;
    }

    public Node toXmlTag(String nameOfElement, Document doc, SchemaMetadata context) {
        if (simpleTypeBase != null) {
            Element elementOfType = doc.createElementNS(namespace, nameOfElement);
            elementOfType.appendChild(BasicTypeUtil.basicTypeNode(simpleTypeBase, doc));
            return elementOfType;
        } else {
            Element elementOfType = doc.createElementNS(namespace, nameOfElement);
            for (ElementType element : elements) {
                elementOfType.appendChild(element.toXmlNode(doc, context));
            }
            return elementOfType;
        }
    }
}
