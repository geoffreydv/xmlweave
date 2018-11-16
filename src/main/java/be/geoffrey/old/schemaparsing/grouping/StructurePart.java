package be.geoffrey.old.schemaparsing.grouping;

import java.util.List;

public interface StructurePart {
    StructurePart copy();
    List<StructurePart> getUnderlyingElements();
}
