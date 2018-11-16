package be.geoffrey.old.schemaparsing.grouping;

import be.geoffrey.old.schemaparsing.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

import static be.geoffrey.old.schemaparsing.AmountOfElementsStrategy.MAX;
import static be.geoffrey.old.schemaparsing.AmountOfElementsStrategy.MIN;
import static be.geoffrey.old.schemaparsing.BasicTypeUtil.BASIC_XSD_TYPES_NAMESPACE;

public class XmlElement implements StructurePart {

    private static final int AS_MUCH_AS_POSSIBLE = 999;

    private String namespace;
    private String name;

    private int minOccurs;
    private int maxOccurs;

    private NameAndNamespace typeReference;

    // OR

    private NameAndNamespace ref;

    private XmlElement() {
    }

    public static XmlElement refElement(NameAndNamespace ref) {
        XmlElement elem = new XmlElement();
        elem.ref = ref;
        return elem;
    }

    public XmlElement(Element element,
                      NameAndNamespace typeReference,
                      String elementNs) {

        if (typeReference == null) {
            throw new IllegalArgumentException("Could not create an element");
        }

        this.name = element.getAttribute("name");
        this.namespace = elementNs;

        String minAttr = element.getAttribute("minOccurs");

        if (StringUtils.isBlank(minAttr)) {
            this.minOccurs = 1;
        } else {
            this.minOccurs = Integer.parseInt(minAttr);
        }

        String maxAttr = element.getAttribute("maxOccurs");

        if (StringUtils.isBlank(maxAttr)) {
            this.maxOccurs = 1;
        } else if (maxAttr.equals("unbounded")) {
            this.maxOccurs = AS_MUCH_AS_POSSIBLE;
        } else {
            this.maxOccurs = Integer.parseInt(maxAttr);
        }

        this.typeReference = typeReference;
    }

    private XmlElement(XmlElement other) {
        this.namespace = other.namespace;
        this.name = other.name;

        this.minOccurs = other.minOccurs;
        this.maxOccurs = other.maxOccurs;

        this.typeReference = other.typeReference;

        this.ref = other.ref;
    }

    @Override
    public StructurePart copy() {
        return new XmlElement(this);
    }

    @Override
    public List<StructurePart> getUnderlyingElements() {
        return new ArrayList<>();
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

    public List<Element> render(Document doc,
                                SchemaParsingContext context,
                                NavNode parentNode,
                                Properties properties,
                                boolean rootElement) {

        if (ref != null) {
            // Draw the reference instead of trying to render this empty element
            XmlElement referencedElement = context.getKnownElement(ref);
            return referencedElement.render(doc, context, parentNode, properties, rootElement);
        }

        NavNode currentPath = new NavNode(parentNode, typeReference, name);

//        System.out.println(currentPath);

        Integer maxRecursionDepth = Integer.valueOf(properties.getProperty("generation.default.maxRecursionDepth"));
        boolean recursing = currentPath.willStartRecursing(maxRecursionDepth);

        if (recursing) {
            System.out.println("WARNING: Recursion detected: " + currentPath + ", breaking after " + maxRecursionDepth + " repetitions");
            return new ArrayList<>();
        }

        int amountOfElementsToRender = decideAmountElementsToRender(currentPath, properties);

        if (BasicTypeUtil.isReferenceToBasicType(typeReference)) {

            List<Element> results = new ArrayList<>();

            for (int i = 0; i < amountOfElementsToRender; i++) {
                Element simpleElement = createElement(doc, rootElement);
                simpleElement.appendChild(BasicTypeUtil.generateContentsOfABasicType(typeReference, doc, properties));
                results.add(simpleElement);
            }

            return results;

        } else {

            NamedStructure structure = context.lookupXmlStructure(typeReference);

            if (structure == null) {
                throw new IllegalArgumentException("Could not load the structure of this class from the XSD context: " + typeReference.identity());
            }

            if (structure.isBasedOnBasicType()) {

                List<Element> elements = new ArrayList<>();
                for (int i = 0; i < amountOfElementsToRender; i++) {
                    Element simpleElement = createElement(doc, rootElement);
                    simpleElement.appendChild(BasicTypeUtil.generateContentsOfACustomBasicType(structure, doc, properties));
                    elements.add(simpleElement);
                }
                return elements;
            }

            SortedSet<NamedStructure> moreSpecificImplementations = findConcreteImplementationCandidates(context, structure);

            if (moreSpecificImplementations.isEmpty()) {
                if (structure.isAbstractType()) {
                    throw new IllegalArgumentException("No implementations were found for abstract class " + structure.identity());
                }

                List<Element> results = new ArrayList<>();

                for (int i = 0; i < amountOfElementsToRender; i++) {
                    Element lemlem = createElement(doc, rootElement);
                    addAttributesAndChildElementsFromStructureToElement(lemlem, doc, context, structure, currentPath, properties);
                    results.add(lemlem);
                }

                return results;
            }

            List<NamedStructure> concreteAsList = new ArrayList<>(moreSpecificImplementations);

            if (!structure.isAbstractType()) {
                concreteAsList.add(0, structure); // Add this type as an option as well as it is not abstract and can be used
            }

            int choiceIndex = getChoiceForDecision(currentPath, properties);
            NamedStructure concreteImplementationChoice = concreteAsList.get(choiceIndex);

            printChoiceMenu(currentPath, choiceIndex, concreteAsList.stream()
                    .map(NamedStructure::getName)
                    .collect(Collectors.toList()), "INHERITANCE");


            List<Element> elements = new ArrayList<>();

            for (int i = 0; i < amountOfElementsToRender; i++) {

                Element lemlem = createElement(doc, rootElement);

                addAttributesAndChildElementsFromStructureToElement(lemlem, doc, context, concreteImplementationChoice, currentPath, properties);

                lemlem.setAttribute("xmlns:xsi", BASIC_XSD_TYPES_NAMESPACE);
                lemlem.setAttribute("xmlns:spec", concreteImplementationChoice.getNamespace());
                lemlem.setAttribute("xsi:type", "spec:" + concreteImplementationChoice.getName());

                elements.add(lemlem);
            }

            return elements;
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

    private void printChoiceMenu(NavNode currentPath, int selectedIndex, List<String> options, final String CHOICE_LABEL) {

        System.out.println("[CHOICE][" + CHOICE_LABEL + "] " + currentPath);

        String choiceMenu = "\tAll choices are: \n";
        for (int i = 0; i < options.size(); i++) {
            String possibility = options.get(i);
            choiceMenu += "\t\t [" + i + "] " + possibility;
            if (selectedIndex == i) {
                choiceMenu += " <-- Selected";
            }
            choiceMenu += "\n";
        }

        System.out.print(choiceMenu);
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
                        .collect(Collectors.toList()), "CHOICE_ELEMENT");

                appendElementsForEveryPartInStructure(me, Lists.newArrayList(selectedPartOfChoice), doc, context, thisNode, properties);
            }
        }
    }

    private void addAttributesAndChildElementsFromStructureToElement(Element element,
                                                                     Document doc,
                                                                     SchemaParsingContext context,
                                                                     NamedStructure structureToUse,
                                                                     NavNode thisNode,
                                                                     Properties properties) {

        for (XmlAttribute xmlAttribute : structureToUse.getAttributes()) {
            element.setAttribute(xmlAttribute.getName(), "RANDOM ATTRIBUTE VALUE"); // TODO: Verder uitwerken / type bepalen etc...
        }

        appendElementsForEveryPartInStructure(element, structureToUse.getStructureParts(), doc, context, thisNode, properties);
    }

    private void renderChildElement(Document doc,
                                    SchemaParsingContext context,
                                    NavNode thisNode,
                                    Element me,
                                    XmlElement xmlElement,
                                    Properties properties) {

        List<Element> elements = xmlElement.render(doc, context, thisNode, properties, false);

        for (Element element : elements) {
            me.appendChild(element);
        }
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

    private int decideAmountElementsToRender(NavNode currentPath, Properties properties) {

        if (minOccurs == maxOccurs) {
            return minOccurs;
        }

        AmountOfElementsStrategy strategy = decideStrategy(properties);

        if (strategy.equals(MIN)) {
            System.out.println("[CHOICE][#] " + currentPath + ". Decided to render " + minOccurs + " elements because the elementStrategy is set to MIN. Range (" + minOccurs + ", " + maxOccurs + ")");
            return minOccurs;
        } else {
            return decideMaxAmountToRender(currentPath, properties);
        }
    }

    private int decideMaxAmountToRender(NavNode currentPath, Properties properties) {
        int maxInProperties = Integer.parseInt(properties.getProperty("generation.default.maxRepetitionsForRepeatableElements"));

        if (maxInProperties < maxOccurs) {
            System.out.println("[CHOICE][#] " + currentPath + ". Decided to render the max amount of elements, " + maxInProperties + " (capped by global config) because the elementStrategy is set to MAX. Range (" + minOccurs + ", " + maxOccurs + ")");
            return maxInProperties;
        }

        System.out.println("[CHOICE][#] " + currentPath + ". Decided to render the max amount of elements, " + maxOccurs + " because the elementStrategy is set to MAX. Range (" + minOccurs + ", " + maxOccurs + ")");
        return maxOccurs;
    }

    private AmountOfElementsStrategy decideStrategy(Properties properties) {
        if (properties.containsKey("generation.default.minOccurs_strategy")) {
            return AmountOfElementsStrategy.valueOf(properties.getProperty("generation.default.minOccurs_strategy"));
        } else {
            return MAX;
        }
    }

    @Override
    public String toString() {
        return "ElementType{" +
                "name='" + name + '\'' +
                ", minOccurs=" + minOccurs +
                ", type=" + typeReference +
                '}';
    }
}