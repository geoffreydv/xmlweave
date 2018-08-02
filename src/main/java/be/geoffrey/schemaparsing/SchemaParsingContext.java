package be.geoffrey.schemaparsing;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SchemaParsingContext {

    // TODO: ik betwijfel dat elements een namespace hebben.. moet waarschijnlijk weg

    private String fileName;

    private Map<String, NamedStructure> knownNamedStructures = new HashMap<>();
    private Map<String, XmlTag> knownElementTypes = new HashMap<>();
    // TODO: Make a "CONCRETE extension of base class" (multiple levels of inheritance nesting with abstracts)
    private Map<String, Set<String>> extensionsOfBaseClass = new HashMap<>();

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
        this.knownNamedStructures.putAll(schemaParsingContext.knownNamedStructures);
        this.knownElementTypes.putAll(schemaParsingContext.knownElementTypes);
    }

    public void addKnownNamedStructure(NamedStructure type) {
        this.knownNamedStructures.put(type.identity(), type);
    }

    public void addKnownRootElement(XmlTag element) {
        this.knownElementTypes.put(element.identity(), element);
    }

    public void indicateElementRequiresInheritanceEnhancement(NamedStructure thisType) {

        classesThatRequireAddingBaseFields.add(thisType.identity());

        String baseClassIdentity = thisType.getExtensionOf().identity();

        if (!extensionsOfBaseClass.containsKey(baseClassIdentity)) {
            extensionsOfBaseClass.put(baseClassIdentity, new HashSet<>());
        }

        this.extensionsOfBaseClass.get(baseClassIdentity).add(thisType.identity());
    }

    public XmlTag getKnownElement(NameAndNamespace ns) {
        return knownElementTypes.get(ns.identity());
    }

    public NamedStructure getKnownXmlType(NameAndNamespace ns) {
        return knownNamedStructures.get(ns.identity());
    }

    public Set<NamedStructure> getExtensionsOfBaseClass(String id) {

        Set<String> concreteImplementationReferences = extensionsOfBaseClass.get(id);

        if (concreteImplementationReferences == null) {
            return new HashSet<>();
        }

        // TODO: Think if also needed for elems?

        return concreteImplementationReferences
                .stream()
                .map(key -> knownNamedStructures.get(key))
                .collect(Collectors.toSet());
    }

    public boolean needsInheritanceEnhancement() {
        return !classesThatRequireAddingBaseFields.isEmpty();
    }

    public void addAllFieldsOfBaseClassesToConcreteImplementations() {

        // TODO: Add a while and make sure it never crashes (definitions in other files etc)

        if (classesThatRequireAddingBaseFields.isEmpty()) {
            return;
        }

        Set<String> remainingKeys = new HashSet<>(classesThatRequireAddingBaseFields);

        for (String key : classesThatRequireAddingBaseFields) {
            // TODO: Add Elements as well?
            NamedStructure xmlTypeToEnhance = knownNamedStructures.get(key);

            NameAndNamespace extension = xmlTypeToEnhance.getExtensionOf();
            NamedStructure baseClass = knownNamedStructures.get(extension.identity());

            if (baseClass != null) {

                // Only add elements once the base class is resolve itself (to support nested inheritance)
                if (!classesThatRequireAddingBaseFields.contains(baseClass.identity())) {
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

    public XmlTag getKnownElementByElementName(String elementName) {

        Set<XmlTag> matches = knownElementTypes.values().stream()
                .filter(e -> e.getName().equals(elementName))
                .collect(Collectors.toSet());

        if (matches.size() == 1) {
            return matches.iterator().next();
        }

        throw new IllegalArgumentException("Could not uniquely identify an element with name " + elementName + ". Names found:" + matches.stream().map(e -> e.getNamespace() + "/" + e.getName()).collect(Collectors.toList()));
    }

    public NamedStructure getKnownTypeByTypeName(String typeName) {

        Set<NamedStructure> matches = knownNamedStructures.values().stream()
                .filter(e -> e.getName().equals(typeName))
                .collect(Collectors.toSet());

        if (matches.size() == 1) {
            return matches.iterator().next();
        }

        throw new IllegalArgumentException("Could not uniquely identify a type with name " + typeName + ". Names found:" + matches.stream().map(e -> e.getNamespace() + "/" + e.getName()).collect(Collectors.toList()));
    }
}
