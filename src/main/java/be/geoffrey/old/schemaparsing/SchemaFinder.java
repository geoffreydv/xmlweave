package be.geoffrey.old.schemaparsing;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class SchemaFinder {

    private Set<String> foundSchemas = new HashSet<>();

    public void build(String sourceDirectory) throws IOException {

        Files.list(Paths.get(sourceDirectory))
                .parallel()
                .forEach(fileLocation -> {
                    if (Files.isDirectory(fileLocation)) {
                        try {
                            build(fileLocation.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (FilenameUtils.getExtension(fileLocation.getFileName().toString()).equals("xsd")) {
                            foundSchemas.add(fileLocation.normalize().toString());
                        }
                    }
                });
    }

    public Set<String> getFoundSchemas() {
        return foundSchemas;
    }
}