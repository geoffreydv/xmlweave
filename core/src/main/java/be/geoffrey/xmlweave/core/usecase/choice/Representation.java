package be.geoffrey.xmlweave.core.usecase.choice;

import java.util.List;
import java.util.Map;

public class Representation {

    private ElementRepresentation rootElement;
    private Map<String, List<Choice>> possibleChoices;

    public Representation(ElementRepresentation rootElement,
                          Map<String, List<Choice>> possibleChoices) {
        this.rootElement = rootElement;
        this.possibleChoices = possibleChoices;
    }

    public ElementRepresentation getRootElement() {
        return rootElement;
    }

    public Map<String, List<Choice>> getPossibleChoices() {
        return possibleChoices;
    }
}
