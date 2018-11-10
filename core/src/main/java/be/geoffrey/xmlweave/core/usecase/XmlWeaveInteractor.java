package be.geoffrey.xmlweave.core.usecase;

import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

public class XmlWeaveInteractor implements XmlWeaveService {

    public XmlWeaveInteractor() {
    }

    @Override
    public List<String> getPossibleElements(File xsdFile) {
        return Lists.newArrayList("SimpleBasicElement", "AnotherSimpleBasicElement");
    }
}
