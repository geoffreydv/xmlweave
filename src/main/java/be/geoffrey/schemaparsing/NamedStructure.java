package be.geoffrey.schemaparsing;

import java.util.ArrayList;
import java.util.List;

public class NamedStructure {

    private String namespace;
    private String name;

    // Attributes of Complex types =======================================

    private boolean abstractType;
    private NameAndNamespace extensionOf;

    // Used when this is a complex type
    private List<XmlElement> elements = new ArrayList<>();

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

    public List<XmlElement> getElements() {
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

    public void addAllElementsAtBeginning(List<XmlElement> elements) {
        this.elements.addAll(0, elements);
    }

    public NameAndNamespace reference() {
        return new NameAndNamespace(name, namespace);
    }
}
