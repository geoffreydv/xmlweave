package be.geoffrey.schemaparsing;

import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public final class BasicTypeUtil {

    private static final List<String> BASIC_TYPES = Lists.newArrayList("string", "int", "boolean");

    private BasicTypeUtil() {
    }

    public static Node basicTypeNode(NameAndNamespace type, Document doc) {
        return basicTypeNode(type, doc, new ArrayList<>());
    }

    public static Node basicTypeNode(NameAndNamespace type, Document doc, List<String> enumValues) {
        switch (type.getName()) {
            case "string":
                if (!enumValues.isEmpty()) {
                    return doc.createTextNode(enumValues.get(0));
                }
                return doc.createTextNode("anystring_anystring");
            case "int":
                return doc.createTextNode("12345");
            case "boolean":
                return doc.createTextNode("true");
        }

        throw new IllegalArgumentException("Unknown basic type: " + type);
    }

    public static boolean isBasicType(NameAndNamespace type) {
        return BASIC_TYPES.contains(type.getName());
    }

    public static Node createBasicTypeElementWithNameAndValue(NameAndNamespace me, NameAndNamespace basicType, Document doc, List<String> enumValues) {
        Element elementOfType = doc.createElementNS(me.getNamespace(), me.getName());
        elementOfType.appendChild(BasicTypeUtil.basicTypeNode(basicType, doc, enumValues));
        return elementOfType;
    }
}
