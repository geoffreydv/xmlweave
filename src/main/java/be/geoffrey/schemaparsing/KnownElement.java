package be.geoffrey.schemaparsing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class KnownElement implements StructureOfClass {

    private String namespace;
    private String name;
    private List<ElementType> elements = new ArrayList<>();

    public KnownElement(String namespace, String name) {
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

    public Element toXmlTag(Document doc, SchemaMetadata context) {

        // TODO: Expand element with basic type logic as well...

        Element document = doc.createElementNS(namespace, name);

        for (ElementType element : elements) {
            document.appendChild(element.toXmlNodeWithName(element.getName(), doc, context));
        }

        return document;
    }
}
