package be.geoffrey.schemaparsing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class KnownXmlType implements StructureOfClass {

    private String namespace;
    private String name;

    private List<ElementType> elements = new ArrayList<>();

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

    public void addElement(ElementType type) {
        elements.add(type);
    }

    public String identity() {
        return namespace + "/" + name;
    }


    public Element toXmlTag(String nameOfElement, Document doc, SchemaMetadata context) {
        Element document = doc.createElementNS(namespace, nameOfElement);

        for (ElementType element : elements) {
            document.appendChild(element.toXmlNode(doc, context));
        }

        return document;
    }
}
