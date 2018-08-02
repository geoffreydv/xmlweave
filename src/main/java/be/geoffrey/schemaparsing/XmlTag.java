package be.geoffrey.schemaparsing;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XmlTag {

    private String namespace;
    private String name;

    private String minOccurs;
    private String maxOccurs;

    private NameAndNamespace structureReference;

    public XmlTag(String namespace, String name, NameAndNamespace structureReference) {
        this.namespace = namespace;
        this.name = name;
        this.structureReference = structureReference;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public NameAndNamespace getStructureReference() {
        return structureReference;
    }

    public void setStructureReference(NameAndNamespace structureReference) {
        this.structureReference = structureReference;
    }

    public String identity() {
        return namespace + "/" + name;
    }

    public Node asXmlTag(Document doc, SchemaParsingContext context) {
        NamedStructure structure = context.getKnownXmlType(structureReference);
        return structure.asXmlTagWithName(name, doc, context);
    }
}