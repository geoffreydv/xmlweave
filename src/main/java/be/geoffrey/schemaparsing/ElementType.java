package be.geoffrey.schemaparsing;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    public ElementType(ElementType elementType) {
        this.name = elementType.name;
        this.minOccurs = elementType.minOccurs;
        this.type = elementType.type;
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

    public Node toXmlNodeWithName(String nameToUse,
                                  Document doc,
                                  SchemaParsingContext context) {

        if (BasicTypeUtil.isBasicType(type)) {
            Element elementOfType = doc.createElement(nameToUse);
            elementOfType.appendChild(BasicTypeUtil.generateContentsOfABasicType(type, doc));
            return elementOfType;
        } else {

            KnownXmlType knownType = context.getKnownXmlType(type);
            // TODO: Replace with "Loop elements"...
            if (knownType == null) {
                System.out.println("NULL");
                throw new IllegalArgumentException("Failed");
            }

            // TODO: Meer keuze-opties
            Set<KnownXmlType> concreteImplementationChoices = findConcreteImplementationCandidates(context, knownType);

            if (!concreteImplementationChoices.isEmpty()) {
                if (knownType.isAbstractType()) {
                    KnownXmlType concreteImplementationChoice = concreteImplementationChoices.iterator().next();
                    System.out.println("A choice was made here to select " + concreteImplementationChoice.getName() + " as the implementation for abstract type " + nameToUse);
                    System.out.println("\tAll choices are: " + concreteImplementationChoices.stream().map(KnownXmlType::getName).collect(Collectors.toList()));
                    return concreteImplementationChoice.asXmlTagWithName(nameToUse, doc, context);
                } else {
                    // TODO: We CAN return this object, or one of its extensions, enable a choice here as well
                    System.out.println("A choice was made here to select " + knownType.getName() + " as the implementation for " + nameToUse + " but a more specific class can be selected");
                    System.out.println("\tAll choices are: " + concreteImplementationChoices.stream().map(KnownXmlType::getName).collect(Collectors.toList()));
                    return knownType.asXmlTagWithName(nameToUse, doc, context);
                }
            } else {
                if (knownType.isAbstractType()) {
                    throw new IllegalArgumentException("No implementations were found for abstract class " + knownType.identity());
                }
            }

            return knownType.asXmlTagWithName(nameToUse, doc, context);
        }
    }

    private Set<KnownXmlType> findConcreteImplementationCandidates(SchemaParsingContext context,
                                                                   KnownXmlType base) {

        Set<KnownXmlType> candidates = context.getExtensionsOfBaseClass(base.identity());
        Set<KnownXmlType> allDiscovered = new HashSet<>(candidates);
        // Keep looping down the hierarchy until all of the concrete classes are discovered

        for (KnownXmlType concreteClass : candidates) {
            allDiscovered.addAll(findConcreteImplementationCandidates(context, concreteClass));
        }

        return allDiscovered;
    }

    @Override
    public String toString() {
        return "ElementType{" +
                "name='" + name + '\'' +
                ", minOccurs=" + minOccurs +
                ", type=" + type +
                '}';
    }
}
