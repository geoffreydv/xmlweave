package be.geoffrey.xmlweave.service;

import be.geoffrey.xmlweave.core.usecase.ElementRepresentation;
import org.springframework.stereotype.Service;

@Service
public class XmlRenderer {

    public String renderAsXml(ElementRepresentation el) {
        return String.format("<%s />", el.getName());
    }

}
