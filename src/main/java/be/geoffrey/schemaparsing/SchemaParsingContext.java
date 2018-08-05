package be.geoffrey.schemaparsing;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SchemaParsingContext {

    private String fileName;

    private Properties properties = new Properties();
    private Map<String, NamedStructure> knownNamedStructures = new HashMap<>();
    private Map<String, XmlElement> knownElementTypes = new HashMap<>();
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
        this.properties.putAll(schemaParsingContext.properties);
        this.parsedFiles.addAll(schemaParsingContext.parsedFiles);
        this.classesThatRequireAddingBaseFields.addAll(schemaParsingContext.classesThatRequireAddingBaseFields);
        this.extensionsOfBaseClass.putAll(schemaParsingContext.extensionsOfBaseClass);
        this.knownNamedStructures.putAll(schemaParsingContext.knownNamedStructures);
        this.knownElementTypes.putAll(schemaParsingContext.knownElementTypes);
    }

    public void addKnownNamedStructure(NamedStructure type) {
        this.knownNamedStructures.put(type.identity(), type);
    }

    public void addKnownRootElement(XmlElement element) {
        this.knownElementTypes.put(element.identity(), element);
    }

    public void indicateElementRequiresInheritanceEnhancement(NamedStructure thisType) {

        classesThatRequireAddingBaseFields.add(thisType.identity());

        String baseClassIdentity = thisType.getExtensionOf().identity();

        if (!extensionsOfBaseClass.containsKey(baseClassIdentity)) {
            extensionsOfBaseClass.put(baseClassIdentity, new TreeSet<>());
        }

        this.extensionsOfBaseClass.get(baseClassIdentity).add(thisType.identity());
    }

    public XmlElement getKnownElement(NameAndNamespace ns) {
        return knownElementTypes.get(ns.identity());
    }

    public NamedStructure lookupXmlStructure(NameAndNamespace ns) {
        return knownNamedStructures.get(ns.identity());
    }

    public SortedSet<NamedStructure> getExtensionsOfBaseClass(String id) {

        Set<String> concreteImplementationReferences = extensionsOfBaseClass.get(id);

        if (concreteImplementationReferences == null) {
            return new TreeSet<>();
        }

        return concreteImplementationReferences
                .stream()
                .map(key -> knownNamedStructures.get(key))
                .sorted()
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean needsInheritanceEnhancement() {
        return !classesThatRequireAddingBaseFields.isEmpty();
    }

    public void addAllDataOfBaseClassesToConcreteImplementations() {

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
                    xmlTypeToEnhance.extendWithDataFromBaseClass(baseClass);
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

    public XmlElement getKnownElementByElementName(String elementName) {

        Set<XmlElement> matches = knownElementTypes.values().stream()
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
