package be.geoffrey.schemaparsing.grouping;

import java.util.List;
import java.util.stream.Collectors;

public class Sequence implements StructurePart {

    private List<StructurePart> parts;

    public Sequence(Sequence sequence) {
        parts = sequence.parts.stream()
                .map(StructurePart::copy)
                .collect(Collectors.toList());
    }

    public Sequence(List<StructurePart> elements) {
        parts = elements.stream()
                .map(StructurePart::copy)
                .collect(Collectors.toList());
    }

    @Override
    public StructurePart copy() {
        return new Sequence(this);
    }

    @Override
    public List<StructurePart> getUnderlyingElements() {
        return parts;
    }
}