package be.geoffrey.xmlweave.core.usecase;

import java.io.File;
import java.util.Optional;

public interface XmlWeaveService {
    Optional<ElementRepresentation> getRepresentation(File xsdFile, String rootElement);
}
