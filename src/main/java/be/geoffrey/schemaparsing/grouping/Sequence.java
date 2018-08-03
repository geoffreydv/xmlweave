package be.geoffrey.schemaparsing.grouping;

import be.geoffrey.schemaparsing.XmlElement;

import java.util.List;

public class Sequence extends ElementGroup {

    public Sequence(Sequence sequence) {
        super(sequence);
    }

    public Sequence(List<XmlElement> elements) {
        super(elements);
    }

    @Override
    public ElementGroup copy() {
        return new Sequence(this);
    }
}