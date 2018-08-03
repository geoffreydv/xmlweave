package be.geoffrey.schemaparsing.grouping;

import be.geoffrey.schemaparsing.XmlElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ElementGroup {

    private List<XmlElement> elements = new ArrayList<>();
    
    public abstract ElementGroup copy();

    public ElementGroup(ElementGroup other) {
        elements.addAll(other.elements.stream()
                .map(XmlElement::new)
                .collect(Collectors.toList()));
    }

    public ElementGroup(List<XmlElement> elements) {
        this.elements = elements;
    }

    public List<XmlElement> getElements() {
        return elements;
    }
}