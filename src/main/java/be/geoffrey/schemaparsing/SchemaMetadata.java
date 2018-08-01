package be.geoffrey.schemaparsing;

import java.util.HashMap;
import java.util.Map;

public class SchemaMetadata {

    // TODO: ik betwijfel dat elements een namespace hebben.. moet waarschijnlijk weg

    private Map<String, KnownXmlType> knownXmlTypes = new HashMap<>();
    private Map<String, KnownElement> knownElementTypes = new HashMap<>();


    public void addKnownXmlType(KnownXmlType type) {
        this.knownXmlTypes.put(type.identity(), type);
    }

    public void addKnownElement(KnownElement element) {
        this.knownElementTypes.put(element.identity(), element);
    }

    public void addInfoFromOtherSchema(SchemaMetadata schemaMetadata) {
        this.knownXmlTypes.putAll(schemaMetadata.knownXmlTypes);
        this.knownElementTypes.putAll(schemaMetadata.knownElementTypes);
    }

    public KnownElement getKnownElement(NameAndNamespace ns) {
        return knownElementTypes.get(ns.identity());
    }

    public KnownXmlType getKnownXmlType(NameAndNamespace ns) {
        return knownXmlTypes.get(ns.identity());
    }
}
