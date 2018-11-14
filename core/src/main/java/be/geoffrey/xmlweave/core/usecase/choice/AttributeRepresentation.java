package be.geoffrey.xmlweave.core.usecase.choice;

public class AttributeRepresentation {

    private final String path;
    private final String name;

    public AttributeRepresentation(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }
}
