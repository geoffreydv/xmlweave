package be.geoffrey.schemaparsing;

import java.util.HashSet;
import java.util.Set;

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

    public boolean willStartRecursing() {
        Set<NameAndNamespace> encounteredTypes = new HashSet<>();

        NavNode cursor = this;
        encounteredTypes.add(typeReference);

        while (cursor.parent != null) {
            cursor = cursor.parent;

            if (encounteredTypes.contains(cursor.typeReference)) {
                return true;
            }

            encounteredTypes.add(cursor.typeReference);
        }

        return false;
    }
}
