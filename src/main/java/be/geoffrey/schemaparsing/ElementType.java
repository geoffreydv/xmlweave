package be.geoffrey.schemaparsing;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ElementType {

    private String name;
    private int minOccurs;
    private NameAndNamespace type;

    public ElementType(String name, String minOccurs, NameAndNamespace type) {
        this.name = name;

        if (StringUtils.isBlank(minOccurs)) {
            this.minOccurs = 1;
        } else {
            this.minOccurs = Integer.parseInt(minOccurs);
        }

        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public NameAndNamespace getType() {
        return type;
    }

    public Node toXmlNode(Document doc, SchemaMetadata context) {

        // TODO: Add namespace shizzle
        switch (type.getName()) {
            case "string":
                return doc.createTextNode("BLABLABLA");
            case "int":
                return doc.createTextNode("12345");
            case "boolean":
                return doc.createTextNode("true");
            default:
                KnownXmlType knownType = context.getKnownXmlType(type);

                if (knownType == null) {
                    System.out.println("NULL");
                    throw new IllegalArgumentException("Failed");
                }

                return knownType.toXmlTag(name, doc, context);
        }
    }
}
