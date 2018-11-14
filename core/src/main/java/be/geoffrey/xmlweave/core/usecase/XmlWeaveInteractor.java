package be.geoffrey.xmlweave.core.usecase;

import be.geoffrey.xmlweave.core.usecase.choice.Choice;
import com.geoffrey.xmlweave.xmlschema.Schema;
import com.geoffrey.xmlweave.xmlschema.TopLevelElement;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.xml.bind.JAXB;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
class XmlWeaveInteractor implements XmlWeaveService {

    public XmlWeaveInteractor() {
    }
//
//    @Override
//    public Representation getRepresentation(File xsdFile, Representation decisions) {
//
//        Schema schema = JAXB.unmarshal(xsdFile, Schema.class);
//
//        Optional<RootElementDecision> rootDecision = decisions.stream()
//                .filter(o -> o.getClass().equals(RootElementDecision.class))
//                .map(o -> (RootElementDecision) o)
//                .findFirst();
//
//        if (!rootDecision.isPresent()) {
//

//        }
//
//        TopLevelElement rootElement = schema.getSimpleTypeOrComplexTypeOrGroup().stream()
//                .filter(e -> e.getClass().equals(TopLevelElement.class))
//                .filter(e -> ((TopLevelElement) e).getName().equals(rootDecision.get().getElementName()))
//                .map(e -> (TopLevelElement) e)
//                .findFirst().get();
//
//
//
//        // Read all root level stuff from this schema
//        /*
//        {@link TopLevelSimpleType }
//         * {@link TopLevelComplexType }
//         * {@link NamedGroup }
//         * {@link NamedAttributeGroup }
//         * {@link TopLevelElement }
//         * {@link TopLevelAttribute }
//         * {@link Notation }
//         * {@link Annotation }
//         */
//
////        for (OpenAttrs node : schema.getSimpleTypeOrComplexTypeOrGroup()) {
////            if (node.getClass().equals(com.geoffrey.xmlweave.xmlschema.TopLevelElement.class)) {
////
////                com.geoffrey.xmlweave.xmlschema.TopLevelElement eleme = (com.geoffrey.xmlweave.xmlschema.TopLevelElement) node;
////
////                if (eleme.getComplexType() != null) {
////                    if (eleme.getComplexType().getSequence() != null) {
////
////                        /*
////
////                        {@link JAXBElement }{@code <}{@link LocalElement }{@code >}
////                         * {@link JAXBElement }{@code <}{@link GroupRef }{@code >}
////                         * {@link JAXBElement }{@code <}{@link All }{@code >}
////                         * {@link JAXBElement }{@code <}{@link ExplicitGroup }{@code >}
////                         * {@link JAXBElement }{@code <}{@link ExplicitGroup }{@code >}
////                         */
////
////                        for (Object particle : eleme.getComplexType().getSequence().getParticle()) {
////                            if (particle instanceof LocalElement) {
////
////                            }
////                        }
////                    }
////                }
////
////            }
////        }
//
////        return topLevelElement;
//        throw new IllegalArgumentException();
//    }

    @Override
    public Optional<ElementRepresentation> getRepresentation(File xsdFile, String rootElement) {

        if (!StringUtils.hasText(rootElement)) {
            return Optional.empty();
        }

        if (StringUtils.hasText(rootElement)) {

            Schema schema = JAXB.unmarshal(xsdFile, Schema.class);

            Optional<TopLevelElement> matchingElement = findAllTopLevelElements(schema)
                    .stream()
                    .filter(elem -> elem.getName().equals(rootElement))
                    .findFirst();

            if (!matchingElement.isPresent()) {
                return Optional.empty();
            }

            return Optional.of(new ElementRepresentation(matchingElement.get().getName()));
        }

//
////        Map<String, List<Choice>> newChoices = copyChoices(choices);
//

//
//        if (newChoices.get("/").isEmpty()) {
//
//            List<TopLevelElement> topLevelElements = findAllTopLevelElements(schema);
//
//            newChoices.get("/").add(new ListChoice(
//                    topLevelElements.stream()
//                            .map(ElementRepresentation::getName)
//                            .collect(Collectors.toList())));
//
//            return new Representation(null, newChoices);
//        }
//
//        ElementChoice elementChoice = newChoices.get("/").stream()
//                .filter(ns -> ns instanceof ElementChoice)
//                .map(ns -> (ElementChoice) ns)
//                .findFirst().get();
//
//        TopLevelElement tle = findAllTopLevelElements(schema).stream()
//                .filter(te -> te.getName().equals(elementChoice.getElement()))
//                .findFirst()
//                .get();
//
//        ElementRepresentation rootElem = new ElementRepresentation("/", tle.getName());
//
//        return new Representation(rootElem, newChoices);
        return Optional.empty();
    }

    private Map<String, List<Choice>> copyChoices(Map<String, List<Choice>> choices) {
        Map<String, List<Choice>> copyOfChoices = new HashMap<>();
        choices.forEach((path, choicesForPath) -> {
            copyOfChoices.put(path, new ArrayList<>(choicesForPath));
        });
        return copyOfChoices;
    }

    private List<TopLevelElement> findAllTopLevelElements(Schema schema) {
        return schema.getSimpleTypeOrComplexTypeOrGroup().stream()
                .filter(e -> e instanceof TopLevelElement)
                .map(e -> (TopLevelElement) e)
                .collect(Collectors.toList());
    }
}