package be.geoffrey.schemaparsing;

import java.util.Map;
import java.util.Objects;

public class NameAndNamespace {

    public static final String SCHEMA_NS = "SCHEMA_NS";

    private String name;
    private String namespace;

    public static NameAndNamespace parse(String name, Map<String, String> knownNamespaces) {
        if (name.contains(":")) {
            String[] parts = name.split(":");
            return new NameAndNamespace(parts[1], knownNamespaces.get(parts[0]));
        } else {
            return new NameAndNamespace(name, knownNamespaces.get(SCHEMA_NS));
        }
    }

    public NameAndNamespace(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public String getBaseName() {
        // TODO: Only use for testing, lame method
        int split = name.indexOf("_");

        if(split == -1) {
            return name;
        }

        return name.substring(split + 1);
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

    @Override
    public String toString() {
        return identity();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameAndNamespace that = (NameAndNamespace) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace);
    }
}
