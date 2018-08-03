package be.geoffrey.schemaparsing;

public class XmlAttribute {

    private String name;
    private NameAndNamespace type;

    public XmlAttribute(String name, NameAndNamespace type) {
        this.name = name;
        this.type = type;
    }

    public XmlAttribute(XmlAttribute other) {
        this.name = other.name;
        this.type = other.type;
    }

    public String getName() {
        return name;
    }

    public NameAndNamespace getType() {
        return type;
    }
}
