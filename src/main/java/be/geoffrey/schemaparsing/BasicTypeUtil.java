package be.geoffrey.schemaparsing;

import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public final class BasicTypeUtil {

    private static final List<String> BASIC_TYPES = Lists.newArrayList("string", "int", "boolean", "double", "decimal", "integer", "dateTime", "date");

    private BasicTypeUtil() {
    }

    public static Node generateContentsOfABasicType(NameAndNamespace type, Document doc) {
        return generateContentsOfACustomBasicType(type, doc, new ArrayList<>(), null);
    }

    public static Node generateContentsOfACustomBasicType(NamedStructure namedStructure, Document doc) {
        return generateContentsOfACustomBasicType(namedStructure.getBasedOnBasicType(),
                doc,
                namedStructure.getPossibleEnumValues(),
                namedStructure.getBasedOnRegex());
    }

    private static Node generateContentsOfACustomBasicType(NameAndNamespace type,
                                                           Document doc,
                                                           List<String> enumValues,
                                                           String regex) {
        switch (type.getName()) {
            case "date":
                return doc.createTextNode("2002-05-30");
            case "dateTime":
                return doc.createTextNode("2002-05-30T09:00:00");
            case "string":
                if (!enumValues.isEmpty()) {
                    return doc.createTextNode(enumValues.get(0));
                } else if (regex != null) {
                    return doc.createTextNode("Something that matches regex: " + regex);
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

    public static boolean isReferenceToBasicType(NameAndNamespace type) {
        // TODO: Move this to reference maybe?
        return BASIC_TYPES.contains(type.getName());
    }
}