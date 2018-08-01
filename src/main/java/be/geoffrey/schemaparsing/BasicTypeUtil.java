package be.geoffrey.schemaparsing;

import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public final class BasicTypeUtil {

    private static final List<String> BASIC_TYPES = Lists.newArrayList("string", "int", "boolean", "double", "decimal", "integer");

    private BasicTypeUtil() {
    }

    public static Node generateContentsOfABasicType(NameAndNamespace type,
                                                    Document doc) {
        return generateContentsOfABasicType(type, doc, new ArrayList<>(), null);
    }

    public static Node generateContentsOfABasicType(NameAndNamespace type,
                                                    Document doc,
                                                    List<String> enumValues,
                                                    String regex) {
        switch (type.getName()) {
            case "string":
                if (!enumValues.isEmpty()) {
                    return doc.createTextNode(enumValues.get(0));
                } else if (regex != null) {
                    return doc.createTextNode("Something that matches " + regex);
                }
                return doc.createTextNode("anystring_anystring");
            case "int":
            case "integer":
                return doc.createTextNode("12345");
            case "boolean":
                return doc.createTextNode("true");
            case "double":
                return doc.createTextNode("12345.65432");
            case "decimal":
                return doc.createTextNode("12345.65432");
        }

        throw new IllegalArgumentException("Unknown basic type: " + type);
    }

    public static boolean isBasicType(NameAndNamespace type) {
        return BASIC_TYPES.contains(type.getName());
    }

    public static Node createBasicTypeElementWithNameAndValue(NameAndNamespace me, NameAndNamespace basicType, Document doc, List<String> enumValues, String basedOnRegex) {
        Element elementOfType = doc.createElementNS(me.getNamespace(), me.getName());
        elementOfType.appendChild(BasicTypeUtil.generateContentsOfABasicType(basicType, doc, enumValues, basedOnRegex));
        return elementOfType;
    }
}
