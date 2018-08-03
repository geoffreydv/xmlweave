package be.geoffrey.schemaparsing;

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
}
