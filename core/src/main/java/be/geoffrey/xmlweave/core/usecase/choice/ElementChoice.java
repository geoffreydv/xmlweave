package be.geoffrey.xmlweave.core.usecase.choice;

public class ElementChoice implements Choice {

    private String element;

    public ElementChoice(String element) {
        this.element = element;
    }

    public String getElement() {
        return element;
    }
}
