package be.geoffrey.schemaparsing.grouping;

import java.util.List;

public class Sequence extends ElementGroup implements StructurePart {

    public Sequence(Sequence sequence) {
        super(sequence);
    }

    public Sequence(List<StructurePart> elements) {
        super(elements);
    }

    @Override
    public StructurePart copy() {
        return new Sequence(this);
    }

    @Override
    public List<StructurePart> getUnderlyingElements() {
        return super.getParts();
    }
}