package be.geoffrey.schemaparsing;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SchemaParsingContext {

    // TODO: ik betwijfel dat elements een namespace hebben.. moet waarschijnlijk weg

    private String fileName;

    private Map<String, KnownXmlType> knownXmlTypes = new HashMap<>();
    private Map<String, KnownElement> knownElementTypes = new HashMap<>();
    private Map<String, List<String>> concreteImplementations = new HashMap<>();

    private Set<String> parsedFiles = new HashSet<>();

    private List<String> keysThatRequireBaseClassEnhancement = new ArrayList<>();

    public SchemaParsingContext(String filePath, SchemaParsingContext previouslyCollectedMetadata) {

        this.fileName = normalizeFileName(filePath);

        if (previouslyCollectedMetadata != null) {
            addInfoFromOtherSchema(previouslyCollectedMetadata);
        }
    }

    private String normalizeFileName(String filePath) {
        return Paths.get(filePath).normalize().toString();
    }

    public void addInfoFromOtherSchema(SchemaParsingContext schemaParsingContext) {
        this.parsedFiles.addAll(schemaParsingContext.parsedFiles);
        this.keysThatRequireBaseClassEnhancement.addAll(schemaParsingContext.keysThatRequireBaseClassEnhancement);
        this.concreteImplementations.putAll(schemaParsingContext.concreteImplementations);
        this.knownXmlTypes.putAll(schemaParsingContext.knownXmlTypes);
        this.knownElementTypes.putAll(schemaParsingContext.knownElementTypes);
    }

    public void addKnownXmlType(KnownXmlType type) {
        this.knownXmlTypes.put(type.identity(), type);
    }

    public void addKnownElement(KnownElement element) {
        this.knownElementTypes.put(element.identity(), element);
    }

    public void indicateElementRequiresInheritanceEnhancement(KnownXmlType thisType) {

        keysThatRequireBaseClassEnhancement.add(thisType.identity());

        String baseClassIdentity = thisType.getExtensionOf().identity();
        if (!concreteImplementations.containsKey(baseClassIdentity)) {
            concreteImplementations.put(baseClassIdentity, new ArrayList<>());
        }

        this.concreteImplementations.get(baseClassIdentity).add(thisType.identity());
    }

    public KnownElement getKnownElement(NameAndNamespace ns) {
        return knownElementTypes.get(ns.identity());
    }

    public KnownXmlType getKnownXmlType(NameAndNamespace ns) {
        return knownXmlTypes.get(ns.identity());
    }

    public List<KnownXmlType> getConcreteImplementationsOfBaseClass(String id) {

        List<String> concreteImplementationReferences = concreteImplementations.get(id);

        if (concreteImplementationReferences == null) {
            return new ArrayList<>();
        }

        // TODO: Think if also needed for elems?

        return concreteImplementationReferences
                .stream()
                .map(key -> knownXmlTypes.get(key))
                .collect(Collectors.toList());
    }

    public void addAllFieldsOfBaseClassesToConcreteImplementations() {

        List<String> remainingKeys = new ArrayList<>();

        for (String key : keysThatRequireBaseClassEnhancement) {
            // TODO: Add Elements as well?
            KnownXmlType xmlTypeToEnhance = knownXmlTypes.get(key);

            NameAndNamespace extension = xmlTypeToEnhance.getExtensionOf();
            KnownXmlType baseClass = knownXmlTypes.get(extension.identity());

            if (baseClass != null) {

                for (ElementType elementType : baseClass.getElements()) {
                    xmlTypeToEnhance.addElement(new ElementType(elementType));
                }

//                System.out.println("Successfully enhanced " + xmlTypeToEnhance.getName() + ", loaded " + baseClass.getElements().size() + " extra fields from base class");
            } else {
                remainingKeys.add(key);
            }
        }

        this.keysThatRequireBaseClassEnhancement = remainingKeys;
    }

    public void indicateFileParsingComplete() {
        parsedFiles.add(this.fileName);
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isSchemaAlreadyParsed(String path) {
        return parsedFiles.contains(normalizeFileName(path));
    }

    public KnownElement getKnownElementByElementName(String elementName) {

        List<KnownElement> matches = knownElementTypes.values().stream()
                .filter(e -> e.getName().equals(elementName))
                .collect(Collectors.toList());

        if (matches.size() == 1) {
            return matches.get(0);
        }

        throw new IllegalArgumentException("Could not uniquely identify an element with name " + elementName);
    }
}
