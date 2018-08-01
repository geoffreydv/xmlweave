package be.geoffrey.schemaparsing;

import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;

public final class BasicTypeUtil {

    private static final List<String> BASIC_TYPES = Lists.newArrayList("string", "int", "boolean");

    private BasicTypeUtil() {
    }

    public static Node basicTypeNode(NameAndNamespace type, Document doc) {
        switch (type.getName()) {
            case "string":
                return doc.createTextNode("BLABLABLA");
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
}
