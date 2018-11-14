package be.geoffrey.xmlweave.core.usecase.choice;

import java.util.ArrayList;
import java.util.List;

public class ElementRepresentation {

    private String path;
    private String name;

    private List<AttributeRepresentation> attributes = new ArrayList<>();
    private List<ElementRepresentation> children = new ArrayList<>();

    public ElementRepresentation(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public List<AttributeRepresentation> getAttributes() {
        return attributes;
    }

    public List<ElementRepresentation> getChildren() {
        return children;
    }
}
