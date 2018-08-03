package be.geoffrey.schemaparsing.grouping;

import be.geoffrey.schemaparsing.XmlElement;

import java.util.List;

public class Choice extends ElementGroup {

    public Choice(Choice sequence) {
        super(sequence);
    }

    public Choice(List<XmlElement> elements) {
        super(elements);
    }

    @Override
    public ElementGroup copy() {
        return new Choice(this);
    }
}