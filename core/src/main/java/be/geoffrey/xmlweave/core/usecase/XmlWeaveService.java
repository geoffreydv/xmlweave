package be.geoffrey.xmlweave.core.usecase;

import java.io.File;
import java.util.List;

public interface XmlWeaveService {
    List<String> getPossibleElements(File testFile);
}
