package be.geoffrey.schemaparsing;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SchemaParsingContext {

    // TODO: ik betwijfel dat elements een namespace hebben.. moet waarschijnlijk weg

    private String fileName;

    private Map<String, KnownXmlType> knownXmlTypes = new HashMap<>();
    private Map<String, KnownElement> knownElementTypes = new HashMap<>();
    // TODO: Make a "CONCRETE extension of base class" (multiple levels of inheritance nesting with abstracts)
    private Map<String, List<String>> extensionsOfBaseClass = new HashMap<>();

    private Set<String> parsedFiles = new HashSet<>();

    private Set<String> classesThatRequireAddingBaseFields = new HashSet<>();

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
        this.classesThatRequireAddingBaseFields.addAll(schemaParsingContext.classesThatRequireAddingBaseFields);
        this.extensionsOfBaseClass.putAll(schemaParsingContext.extensionsOfBaseClass);
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

        classesThatRequireAddingBaseFields.add(thisType.identity());

        String baseClassIdentity = thisType.getExtensionOf().identity();

        if (!extensionsOfBaseClass.containsKey(baseClassIdentity)) {
            extensionsOfBaseClass.put(baseClassIdentity, new ArrayList<>());
        }

        this.extensionsOfBaseClass.get(baseClassIdentity).add(thisType.identity());
    }

    public KnownElement getKnownElement(NameAndNamespace ns) {
        return knownElementTypes.get(ns.identity());
    }

    public KnownXmlType getKnownXmlType(NameAndNamespace ns) {
        return knownXmlTypes.get(ns.identity());
    }

    public List<KnownXmlType> getExtensionsOfBaseClass(String id) {

        List<String> concreteImplementationReferences = extensionsOfBaseClass.get(id);

        if (concreteImplementationReferences == null) {
            return new ArrayList<>();
        }

        // TODO: Think if also needed for elems?

        return concreteImplementationReferences
                .stream()
                .map(key -> knownXmlTypes.get(key))
                .collect(Collectors.toList());
    }

    public boolean needsInheritanceEnhancement() {
        return !classesThatRequireAddingBaseFields.isEmpty();
    }

    public void addAllFieldsOfBaseClassesToConcreteImplementations() {

        // TODO: Add a while and make sure it never crashes (definitions in other files etc)

        if(classesThatRequireAddingBaseFields.isEmpty()) {
            return;
        }

        Set<String> remainingKeys = new HashSet<>(classesThatRequireAddingBaseFields);

        for (String key : classesThatRequireAddingBaseFields) {
            // TODO: Add Elements as well?
            KnownXmlType xmlTypeToEnhance = knownXmlTypes.get(key);

            NameAndNamespace extension = xmlTypeToEnhance.getExtensionOf();
            KnownXmlType baseClass = knownXmlTypes.get(extension.identity());

            if (baseClass != null) {

                // Only add elements once the base class is resolve itself (to support nested inheritance)
                if(!classesThatRequireAddingBaseFields.contains(baseClass.identity())) {
                    xmlTypeToEnhance.addAllElementsAtBeginning(baseClass.getElements().stream()
                            .map(ElementType::new)
                            .collect(Collectors.toList()));
                    remainingKeys.remove(key);
                }
            }
        }

        this.classesThatRequireAddingBaseFields = remainingKeys;
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

        Set<KnownElement> matches = knownElementTypes.values().stream()
                .filter(e -> e.getName().equals(elementName))
                .collect(Collectors.toSet());

        if (matches.size() == 1) {
            return matches.iterator().next();
        }

        throw new IllegalArgumentException("Could not uniquely identify an element with name " + elementName + ". Names found:" + matches.stream().map(e -> e.getNamespace() + "/" + e.getName()).collect(Collectors.toList()));
    }

    public KnownXmlType getKnownTypeByTypeName(String typeName) {

        Set<KnownXmlType> matches = knownXmlTypes.values().stream()
                .filter(e -> e.getName().equals(typeName))
                .collect(Collectors.toSet());

        if (matches.size() == 1) {
            return matches.iterator().next();
        }

        throw new IllegalArgumentException("Could not uniquely identify a type with name " + typeName + ". Names found:" + matches.stream().map(e -> e.getNamespace() + "/" + e.getName()).collect(Collectors.toList()));
    }
}
