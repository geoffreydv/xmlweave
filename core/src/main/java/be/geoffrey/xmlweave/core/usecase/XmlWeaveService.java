package be.geoffrey.xmlweave.core.usecase;

import be.geoffrey.xmlweave.core.usecase.choice.Choice;
import be.geoffrey.xmlweave.core.usecase.choice.Representation;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface XmlWeaveService {
    Representation getRepresentation(File xsdFile, Map<String, List<Choice>> choices);
}
