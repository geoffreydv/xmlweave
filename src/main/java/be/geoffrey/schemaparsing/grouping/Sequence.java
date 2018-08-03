package be.geoffrey.schemaparsing.grouping;

import be.geoffrey.schemaparsing.XmlElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Sequence {

    private List<XmlElement> elements = new ArrayList<>();

    public Sequence() {
    }

    public Sequence(Sequence other) {
        Sequence sequence = new Sequence();
        sequence.elements.addAll(other.elements.stream()
                .map(XmlElement::new)
                .collect(Collectors.toList()));
    }

    public Sequence(List<XmlElement> elements) {
        this.elements = elements;
    }

    public List<XmlElement> getElements() {
        return elements;
    }
}