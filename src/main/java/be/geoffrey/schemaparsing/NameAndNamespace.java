package be.geoffrey.schemaparsing;

public class NameAndNamespace {

    private String name;
    private String namespace;

    public NameAndNamespace(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String identity() {
        return namespace + "/" + name;
    }
}
