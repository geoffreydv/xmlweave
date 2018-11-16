package be.geoffrey.old.schemaparsing;

import be.geoffrey.old.schemaparsing.grouping.StructurePart;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NamedStructure implements Comparable<NamedStructure> {

    private String namespace;
    private String name;

    // Attributes of Complex types =======================================

    private boolean abstractType;
    private NameAndNamespace extensionOf;

    // Used when this is a complex type
    private List<StructurePart> structureParts = new ArrayList<>();

    private List<XmlAttribute> attributes = new ArrayList<>();

    // Attributes of simple types =======================================

    private NameAndNamespace basedOnBasicType;
    private List<String> possibleEnumValues = new ArrayList<>();
    private String basedOnRegex;

    public NamedStructure(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public String getBaseName() {
        // TODO: Only use for testing, lame method
        int split = name.indexOf("_");

        if (split == -1) {
            return name;
        }

        return name.substring(split + 1);
    }

    public List<XmlAttribute> getAttributes() {
        return attributes;
    }

    public List<String> getPossibleEnumValues() {
        return possibleEnumValues;
    }

    public String getBasedOnRegex() {
        return basedOnRegex;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public List<StructurePart> getStructureParts() {
        return structureParts;
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

    public boolean isExtensionOfOtherCustomType() {
        return extensionOf != null;
    }

    public String identity() {
        return namespace + "/" + name;
    }

    public void addStructurePartsAtBeginning(List<StructurePart> structureParts) {
        this.structureParts.addAll(0, structureParts);
    }

    public void addAllAttributesAtBeginning(List<XmlAttribute> attributes) {
        this.attributes.addAll(0, attributes);
    }

    public NameAndNamespace reference() {
        return new NameAndNamespace(name, namespace);
    }

    @Override
    public String toString() {
        return identity();
    }

    public boolean isBasedOnBasicType() {
        return basedOnBasicType != null;
    }

    public void extendWithDataFromBaseClass(NamedStructure baseClass) {

        addStructurePartsAtBeginning(baseClass.getStructureParts().stream()
                .map(StructurePart::copy)
                .collect(Collectors.toList()));

        addAllAttributesAtBeginning(baseClass.getAttributes().stream()
                .map(XmlAttribute::new)
                .collect(Collectors.toList()));
    }

    @Override
    public int compareTo(NamedStructure o) {
        int nsCompare = namespace.compareTo(o.namespace);

        if (nsCompare != 0) {
            return nsCompare;
        }

        return name.compareTo(o.name);
    }
}