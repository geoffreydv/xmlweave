package be.geoffrey.schemaparsing;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class XmlElement {

    private String namespace;
    private String name;

    private String minOccurs;
    private String maxOccurs;

    private NameAndNamespace structureReference;

    public XmlElement(String namespace, String name, NameAndNamespace structureReference) {

        if (StringUtils.isBlank(name) || StringUtils.isBlank(name) || structureReference == null) {
            throw new IllegalArgumentException("Could not create an element");
        }

        this.name = name;
        this.namespace = namespace;
        this.structureReference = structureReference;
    }

    public XmlElement(XmlElement other) {
        this.namespace = other.namespace;
        this.name = other.name;

        this.minOccurs = other.minOccurs;
        this.maxOccurs = other.maxOccurs;

        this.structureReference = other.structureReference;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String identity() {
        return namespace + "/" + name;
    }

    public Element render(Document doc, SchemaParsingContext context) {

        if (BasicTypeUtil.isReferenceToBasicType(structureReference)) {

            Element simpleElement = doc.createElement(name);
            simpleElement.appendChild(BasicTypeUtil.generateContentsOfABasicType(structureReference, doc));
            return simpleElement;

        } else {

            // Fetch the structure from the context
            NamedStructure structure = context.lookupXmlStructure(structureReference);

            if (structure == null) {
                throw new IllegalArgumentException("Could not load the structure of this class from the XSD context.");
            }

            if (structure.isBasedOnBasicType()) {
                Element simpleElement = doc.createElement(name);
                simpleElement.appendChild(BasicTypeUtil.generateContentsOfACustomBasicType(structure, doc));
                return simpleElement;
            }

            Set<NamedStructure> concreteImplementationChoices = findConcreteImplementationCandidates(context, structure);

            if (concreteImplementationChoices.isEmpty()) {

                // Can this element be defined in multiple ways?

                if (structure.isAbstractType()) {
                    throw new IllegalArgumentException("No implementations were found for abstract class " + structure.identity());
                }

                return buildElementFromStructure(doc, context, structure);

            }

            if (structure.isAbstractType()) {
                NamedStructure concreteImplementationChoice = concreteImplementationChoices.iterator().next();
                System.out.println("A choice was made here to select " + concreteImplementationChoice.getName() + " as the implementation for element '" + name + "' of type '" + structure.getName() + "'");
                System.out.println("\tAll choices are: " + concreteImplementationChoices.stream().map(NamedStructure::getName).collect(Collectors.toList()));
                return buildElementFromStructure(doc, context, concreteImplementationChoice);
            } else {
                // TODO: We CAN return this object, or one of its extensions, enable a choice here as well
                System.out.println("A choice was made here to select " + structure.getName() + " as the implementation for " + name + " but a more specific class can be selected");
                System.out.println("\tAll choices are: " + concreteImplementationChoices.stream().map(NamedStructure::getName).collect(Collectors.toList()));
                return buildElementFromStructure(doc, context, structure);
            }

        }
    }

    private Element buildElementFromStructure(Document doc,
                                              SchemaParsingContext context,
                                              NamedStructure structureToUse) {

        Element me = doc.createElement(name);

        for (XmlElement xmlElement : structureToUse.getElements()) {
            me.appendChild(xmlElement.render(doc, context));
        }

        return me;
    }

    public String getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(String minOccurs) {
        this.minOccurs = minOccurs;
    }

    public String getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(String maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    private Set<NamedStructure> findConcreteImplementationCandidates(SchemaParsingContext context,
                                                                     NamedStructure base) {

        Set<NamedStructure> candidates = context.getExtensionsOfBaseClass(base.identity());
        Set<NamedStructure> allDiscovered = new HashSet<>(candidates);
        // Keep looping down the hierarchy until all of the concrete classes are discovered

        for (NamedStructure concreteClass : candidates) {
            allDiscovered.addAll(findConcreteImplementationCandidates(context, concreteClass));
        }

        return allDiscovered;
    }

    @Override
    public String toString() {
        return "ElementType{" +
                "name='" + name + '\'' +
                ", minOccurs=" + minOccurs +
                ", type=" + structureReference +
                '}';
    }
}