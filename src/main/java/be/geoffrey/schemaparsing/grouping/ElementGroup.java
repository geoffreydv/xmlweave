package be.geoffrey.schemaparsing.grouping;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ElementGroup {

    private List<StructurePart> parts;

    public ElementGroup(ElementGroup other) {
        parts = other.parts.stream()
                .map(StructurePart::copy)
                .collect(Collectors.toList());
    }

    public ElementGroup(List<StructurePart> structureParts) {
        this.parts = structureParts;
    }

    public List<StructurePart> getParts() {
        return parts;
    }
}