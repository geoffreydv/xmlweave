package be.geoffrey.schemaparsing.grouping;

import java.util.List;

public class Choice extends ElementGroup implements StructurePart {

    public Choice(Choice sequence) {
        super(sequence);
    }

    public Choice(List<StructurePart> elements) {
        super(elements);
    }

    @Override
    public StructurePart copy() {
        return new Choice(this);
    }

    @Override
    public List<StructurePart> getUnderlyingElements() {
        return super.getParts();
    }
}