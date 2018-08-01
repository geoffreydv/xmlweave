package be.geoffrey.schemaparsing;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

public class ElementType {

    private String name;
    private int minOccurs;
    private NameAndNamespace type;

    public ElementType(String name, String minOccurs, NameAndNamespace type) {
        this.name = name;

        if (StringUtils.isBlank(minOccurs)) {
            this.minOccurs = 1;
        } else {
            this.minOccurs = Integer.parseInt(minOccurs);
        }

        this.type = type;
    }

    public ElementType(ElementType elementType) {
        this.name = elementType.name;
        this.minOccurs = elementType.minOccurs;
        this.type = elementType.type;
    }

    public String getName() {
        return name;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public NameAndNamespace getType() {
        return type;
    }

    public Node toXmlNodeWithName(String nameToUse,
                                  Document doc,
                                  SchemaParsingContext context) {

        // TODO: Add namespace shizzle

        if (BasicTypeUtil.isBasicType(type)) {
            Element elementOfType = doc.createElement(nameToUse);
            elementOfType.appendChild(BasicTypeUtil.generateContentsOfABasicType(type, doc));
            return elementOfType;
        } else {

            KnownXmlType knownType = context.getKnownXmlType(type);
            // TODO: Replace with "Loop elements"...
            if (knownType == null) {
                System.out.println("NULL");
                throw new IllegalArgumentException("Failed");
            }

            // TODO: Meer keuze-opties
            if (knownType.isAbstractType()) {
                List<KnownXmlType> concreteImplementationChoices = context.getConcreteImplementationsOfBaseClass(knownType.identity());

                if (concreteImplementationChoices.isEmpty()) {
                    throw new IllegalArgumentException("No implementations were found for abstract class " + knownType.identity());
                }

                return concreteImplementationChoices.get(0).asXmlTagWithName(nameToUse, doc, context);
            }

            return knownType.asXmlTagWithName(nameToUse, doc, context);
        }
    }

    @Override
    public String toString() {
        return "ElementType{" +
                "name='" + name + '\'' +
                ", minOccurs=" + minOccurs +
                ", type=" + type +
                '}';
    }
}
