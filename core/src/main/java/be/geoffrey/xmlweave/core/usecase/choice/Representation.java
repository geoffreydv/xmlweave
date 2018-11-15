package be.geoffrey.xmlweave.core.usecase.choice;

import be.geoffrey.xmlweave.core.usecase.Element;

import java.util.List;
import java.util.Map;

public class Representation {

    private Element rootElement;
    private Map<String, List<Choice>> possibleChoices;

    public Representation(Element rootElement,
                          Map<String, List<Choice>> possibleChoices) {
        this.rootElement = rootElement;
        this.possibleChoices = possibleChoices;
    }

    public Element getRootElement() {
        return rootElement;
    }

    public Map<String, List<Choice>> getPossibleChoices() {
        return possibleChoices;
    }
}
