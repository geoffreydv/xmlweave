package be.geoffrey.schemaparsing;

import java.util.*;

public class NavNode {

    private NameAndNamespace typeReference;
    private String path;

    private NavNode parent;

    public NavNode(NavNode previous,
                   NameAndNamespace typeRef,
                   String path) {

        this.parent = previous;
        this.typeReference = typeRef;
        this.path = path;
    }

    public String asText() {

        String representation = "";

        if (parent != null) {
            representation = parent.asText();
        }

        return representation + "/" + path;
    }

    @Override
    public String toString() {
        return asText();
    }

    public String typePath() {

        String representation = "";

        if (parent != null) {
            representation = parent.typePath();
        }

        return representation + "/" + typeReference.getName();
    }

    public boolean willStartRecursing(int maxDepth) {

        Map<NameAndNamespace, Integer> encounteredCounters = new HashMap<>();

        NavNode cursor = this;

        encounteredCounters.putIfAbsent(cursor.typeReference, 0);

        Integer originalCount = encounteredCounters.get(cursor.typeReference);
        encounteredCounters.put(cursor.typeReference, originalCount + 1);

        while (cursor.parent != null) {
            cursor = cursor.parent;

            encounteredCounters.putIfAbsent(cursor.typeReference, 0);

            if (encounteredCounters.get(cursor.typeReference) > (maxDepth - 1)) {
                return true;
            }

            originalCount = encounteredCounters.get(cursor.typeReference);
            encounteredCounters.put(cursor.typeReference, originalCount + 1);
        }

        return false;
    }
}
