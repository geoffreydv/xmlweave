package be.geoffrey.schemaparsing;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
