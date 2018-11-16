package be.geoffrey.old.schemaparsing;

import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class BasicTypeUtil {

    public static final String BASIC_XSD_TYPES_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final List<String> BASIC_TYPES = Lists.newArrayList("string", "int", "boolean", "double", "decimal", "integer", "dateTime", "date");

    private BasicTypeUtil() {
    }

    public static Node generateContentsOfABasicType(NameAndNamespace basicType, Document doc, Properties properties) {
        return generateContentsOfACustomBasicType(basicType, null, doc, new ArrayList<>(), null, properties);
    }

    public static Node generateContentsOfACustomBasicType(NamedStructure namedStructure, Document doc, Properties properties) {
        return generateContentsOfACustomBasicType(namedStructure.getBasedOnBasicType(),
                namedStructure.reference(),
                doc,
                namedStructure.getPossibleEnumValues(),
                namedStructure.getBasedOnRegex(), properties);
    }

    private static Node generateContentsOfACustomBasicType(NameAndNamespace basicType,
                                                           NameAndNamespace ownExtensionOfBasicType,
                                                           Document doc,
                                                           List<String> enumValues,
                                                           String regex,
                                                           Properties properties) {
        switch (basicType.getName()) {
            case "date":
                return doc.createTextNode("2002-05-30");
            case "dateTime":
                return doc.createTextNode("2002-05-30T09:00:00");
            case "string":
                if (!enumValues.isEmpty()) {
                    return doc.createTextNode(enumValues.get(0));
                } else if (regex != null) {
                    if (ownExtensionOfBasicType != null) {

                        if (properties.containsKey("regexes." + ownExtensionOfBasicType.getName())) {
                            return doc.createTextNode(properties.getProperty("regexes." + ownExtensionOfBasicType.getName()));
                        }

                        return doc.createTextNode("Something that matches regex pattern of '" + ownExtensionOfBasicType.getName() + "': " + regex);
                    } else {
                        return doc.createTextNode("Something that matches regex pattern " + regex);
                    }
                }
                return doc.createTextNode("anystring");
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

        throw new IllegalArgumentException("Unknown basic type: " + basicType);
    }

    public static boolean isReferenceToBasicType(NameAndNamespace type) {
        // TODO: Move this to reference maybe?
        return BASIC_XSD_TYPES_NAMESPACE.equals(type.getNamespace())
                && BASIC_TYPES.contains(type.getName());
    }
}