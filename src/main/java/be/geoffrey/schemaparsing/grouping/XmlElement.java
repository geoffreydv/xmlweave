package be.geoffrey.schemaparsing.grouping;

import be.geoffrey.schemaparsing.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

public class XmlElement implements StructurePart {

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

    @Override
    public StructurePart copy() {
        return new XmlElement(this);
    }

    @Override
    public List<StructurePart> getUnderlyingElements() {
        return new ArrayList<>();
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

    public Element render(Document doc, SchemaParsingContext context, NavNode parentNode, Properties properties) {

        NavNode currentPath = new NavNode(parentNode, structureReference, name);

        boolean recursing = currentPath.willStartRecursing();

        if (recursing) {
            System.out.println("WARNING: Recursion detected: " + currentPath);
            return null;
        }

        if (BasicTypeUtil.isReferenceToBasicType(structureReference)) {

            Element simpleElement = doc.createElement(name);
            simpleElement.appendChild(BasicTypeUtil.generateContentsOfABasicType(structureReference, doc));
            return simpleElement;

        } else {

            NamedStructure structure = context.lookupXmlStructure(structureReference);

            if (structure == null) {
                throw new IllegalArgumentException("Could not load the structure of this class from the XSD context: " + structureReference.identity());
            }

            if (structure.isBasedOnBasicType()) {
                Element simpleElement = doc.createElement(name);
                simpleElement.appendChild(BasicTypeUtil.generateContentsOfACustomBasicType(structure, doc));
                return simpleElement;
            }

            SortedSet<NamedStructure> concreteImplementationChoices = findConcreteImplementationCandidates(context, structure);

            if (concreteImplementationChoices.isEmpty()) {

                // Can this element be defined in multiple ways?

                if (structure.isAbstractType()) {
                    throw new IllegalArgumentException("No implementations were found for abstract class " + structure.identity());
                }

                return buildElementFromStructure(doc, context, structure, currentPath, properties);
            }

            int choiceIndex = 0;

            List<NamedStructure> concreteAsList = new ArrayList<>(concreteImplementationChoices);

            if (structure.isAbstractType()) {
                NamedStructure concreteImplementationChoice = concreteAsList.get(choiceIndex);

                System.out.println("[CHOICE] " + currentPath);

                String choiceOverview = "\tAll choices are: \n";

                for (int i = 0; i < concreteAsList.size(); i++) {
                    NamedStructure possibility = concreteAsList.get(i);
                    choiceOverview += "\t\t [" + i + "] " + possibility.getName();
                    if (choiceIndex == i) {
                        choiceOverview += " <-- Selected";
                    }
                    choiceOverview += "\n";
                }

                System.out.println(choiceOverview);
                return buildElementFromStructure(doc, context, concreteImplementationChoice, currentPath, properties);
            } else {
                System.out.println("[CHOICE] " + currentPath + ": Selected " + structure.getName() + " as the implementation for " + structure.getName() + " but a more specific class can be selected");
                System.out.println("\tOther choices are: " + concreteImplementationChoices.stream().map(NamedStructure::getName).collect(Collectors.toList()));
                return buildElementFromStructure(doc, context, structure, currentPath, properties);
            }
        }
    }

    private void appendElementsForEveryPartInStructure(Element me,
                                                       List<StructurePart> structureParts,
                                                       Document doc,
                                                       SchemaParsingContext context,
                                                       NavNode thisNode,
                                                       Properties properties) {

        for (StructurePart structurePart : structureParts) {
            if (XmlElement.class.isAssignableFrom(structurePart.getClass())) {
                // Sometimes it's a plain element...
                renderChildElement(doc, context, thisNode, me, (XmlElement) structurePart, properties);

            } else if (Sequence.class.isAssignableFrom(structurePart.getClass())) {
                // Sometimes it's a sequence, in this case we append every item in the sequence
                List<StructurePart> partsInSequence = structurePart.getUnderlyingElements();
                appendElementsForEveryPartInStructure(me, partsInSequence, doc, context, thisNode, properties);

            } else if (Choice.class.isAssignableFrom(structurePart.getClass())) {

                // TODO: Check inheritance here.. I think that when 1st item of a choice is abstract for example, implementations are not searched...
                // Pick the first one... // TODO: Add selecting behavior here
                List<StructurePart> partsAvailableToChooseFrom = structurePart.getUnderlyingElements();
                StructurePart firstPart = partsAvailableToChooseFrom.get(0);
                appendElementsForEveryPartInStructure(me, Lists.newArrayList(firstPart), doc, context, thisNode, properties);

                System.out.println("HMM");
                // TODO: Find a way to visualize this (a choice can contain 2 elements and a sequence for example), working with an index would be cool
                System.out.println("[CHOICE] Made a choice for " + thisNode);
//                List<String> allPossibilities = firstPart.getUnderlyingElements().stream()
//                        .map(XmlElement::getName)
//                        .collect(Collectors.toList());
//
//                System.out.println("[CHOICE] " + thisNode + ": Selected element " + elementChoice.getName() + " as the choice for " + structureToUse.getName() + ".");
//                System.out.println("\tThe other choices are: " + allPossibilities);
//
//                renderChildElement(doc, context, thisNode, me, elementChoice);
            }
        }
    }

    private Element buildElementFromStructure(Document doc,
                                              SchemaParsingContext context,
                                              NamedStructure structureToUse,
                                              NavNode thisNode,
                                              Properties properties) {

        Element me = doc.createElement(name);

        for (XmlAttribute xmlAttribute : structureToUse.getAttributes()) {
            me.setAttribute(xmlAttribute.getName(), "RANDOM ATTRIBUTE VALUE"); // TODO: Verder uitwerken / type bepalen etc...
        }

        appendElementsForEveryPartInStructure(me, structureToUse.getStructureParts(), doc, context, thisNode, properties);

        return me;
    }

    private void renderChildElement(Document doc,
                                    SchemaParsingContext context,
                                    NavNode thisNode,
                                    Element me,
                                    XmlElement xmlElement,
                                    Properties properties) {

        Element element = xmlElement.render(doc, context, thisNode, properties);
        if (element != null) {
            me.appendChild(element);
        }
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

    private SortedSet<NamedStructure> findConcreteImplementationCandidates(SchemaParsingContext context,
                                                                           NamedStructure base) {

        Set<NamedStructure> candidates = context.getExtensionsOfBaseClass(base.identity());
        SortedSet<NamedStructure> allDiscovered = new TreeSet<>(candidates);
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