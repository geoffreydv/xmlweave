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

    public XmlElement(Element element,
                      NameAndNamespace structureReference,
                      String elementNs) {

        if (structureReference == null) {
            throw new IllegalArgumentException("Could not create an element");
        }

        this.name = element.getAttribute("name");
        this.namespace = elementNs;
        this.minOccurs = element.getAttribute("minOccurs");
        this.maxOccurs = element.getAttribute("maxOccurs");

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

    public Element render(Document doc, SchemaParsingContext context, NavNode parentNode, Properties properties, boolean rootElement) {

        NavNode currentPath = new NavNode(parentNode, structureReference, name);

        boolean recursing = currentPath.willStartRecursing();

        if (recursing) {
            System.out.println("WARNING: Recursion detected: " + currentPath + ", breaking after one repetition");
            return null;
        }

        if (BasicTypeUtil.isReferenceToBasicType(structureReference)) {

            Element simpleElement = createElement(doc, rootElement);
            simpleElement.appendChild(BasicTypeUtil.generateContentsOfABasicType(structureReference, doc, properties));
            return simpleElement;

        } else {

            NamedStructure structure = context.lookupXmlStructure(structureReference);

            if (structure == null) {
                throw new IllegalArgumentException("Could not load the structure of this class from the XSD context: " + structureReference.identity());
            }

            if (structure.isBasedOnBasicType()) {
                Element simpleElement = createElement(doc, rootElement);
                simpleElement.appendChild(BasicTypeUtil.generateContentsOfACustomBasicType(structure, doc, properties));
                return simpleElement;
            }

            SortedSet<NamedStructure> moreSpecificImplementations = findConcreteImplementationCandidates(context, structure);

            if (moreSpecificImplementations.isEmpty()) {
                if (structure.isAbstractType()) {
                    throw new IllegalArgumentException("No implementations were found for abstract class " + structure.identity());
                }

                return buildElementFromStructure(doc, context, structure, currentPath, properties, rootElement);
            }

            List<NamedStructure> concreteAsList = new ArrayList<>(moreSpecificImplementations);

            if (!structure.isAbstractType()) {
                concreteAsList.add(0, structure); // Add this type as an option as well as it is not abstract and can be used
            }

            int choiceIndex = getChoiceForDecision(currentPath, properties);
            NamedStructure concreteImplementationChoice = concreteAsList.get(choiceIndex);

            printChoiceMenu(currentPath, choiceIndex, concreteAsList.stream()
                    .map(NamedStructure::getName)
                    .collect(Collectors.toList()));

            Element element = buildElementFromStructure(doc, context, concreteImplementationChoice, currentPath, properties, false);

            element.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            element.setAttribute("xmlns:spec", concreteImplementationChoice.getNamespace());
            element.setAttribute("xsi:type", "spec:" + concreteImplementationChoice.getName());

            return element;
        }
    }

    private Element createElement(Document doc, boolean rootElement) {
        if (rootElement && StringUtils.isNotBlank(namespace)) {
            return doc.createElementNS(namespace, "tmp:" + name);
        } else {
            return doc.createElement(name);
        }
    }

    private int getChoiceForDecision(NavNode currentPath, Properties properties) {

        if (properties.containsKey(currentPath + ".choice")) {
            String index = properties.getProperty(currentPath + ".choice");
            return Integer.valueOf(index);
        }

        return 0;
    }

    private void printChoiceMenu(NavNode currentPath, int selectedIndex, List<String> options) {

        System.out.println("[CHOICE] " + currentPath);

        String choiceMenu = "\tAll choices are: \n";
        for (int i = 0; i < options.size(); i++) {
            String possibility = options.get(i);
            choiceMenu += "\t\t [" + i + "] " + possibility;
            if (selectedIndex == i) {
                choiceMenu += " <-- Selected";
            }
            choiceMenu += "\n";
        }

        System.out.println(choiceMenu);
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
                List<StructurePart> partsAvailableToChooseFrom = structurePart.getUnderlyingElements();

                int choice = getChoiceForDecision(thisNode, properties);
                StructurePart selectedPartOfChoice = partsAvailableToChooseFrom.get(choice);

                printChoiceMenu(thisNode, choice, partsAvailableToChooseFrom
                        .stream()
                        .map(part -> {
                            if (XmlElement.class.isAssignableFrom(part.getClass())) {
                                return "An element named '" + ((XmlElement) part).getName() + "'";
                            } else if (Choice.class.isAssignableFrom(part.getClass())) {
                                return "a 'choice' element";
                            } else if (Sequence.class.isAssignableFrom(part.getClass())) {
                                return "a 'sequence' element";
                            }
                            return "Unknown choice";
                        })
                        .collect(Collectors.toList()));

                appendElementsForEveryPartInStructure(me, Lists.newArrayList(selectedPartOfChoice), doc, context, thisNode, properties);
            }
        }
    }

    private Element buildElementFromStructure(Document doc,
                                              SchemaParsingContext context,
                                              NamedStructure structureToUse,
                                              NavNode thisNode,
                                              Properties properties,
                                              boolean rootElement) {


        Element me = createElement(doc, rootElement);

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

        Element element = xmlElement.render(doc, context, thisNode, properties, false);
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