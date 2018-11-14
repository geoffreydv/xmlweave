package be.geoffrey.xmlweave.core.usecase.decision;

import be.geoffrey.xmlweave.core.usecase.decision.Decision;

public class RootElementDecision implements Decision {

    private String elementName;

    public RootElementDecision(String elementName) {
        this.elementName = elementName;
    }

    public String getElementName() {
        return elementName;
    }
}
