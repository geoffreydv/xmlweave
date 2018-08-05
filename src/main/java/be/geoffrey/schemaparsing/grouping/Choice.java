package be.geoffrey.schemaparsing.grouping;

import java.util.List;
import java.util.stream.Collectors;

public class Choice implements StructurePart {

    private List<StructurePart> parts;

    public Choice(Choice choice) {
        parts = choice.parts.stream()
                .map(StructurePart::copy)
                .collect(Collectors.toList());
    }

    public Choice(List<StructurePart> elements) {
        parts = elements.stream()
                .map(StructurePart::copy)
                .collect(Collectors.toList());
    }

    @Override
    public StructurePart copy() {
        return new Choice(this);
    }

    @Override
    public List<StructurePart> getUnderlyingElements() {
        return parts;
    }
}