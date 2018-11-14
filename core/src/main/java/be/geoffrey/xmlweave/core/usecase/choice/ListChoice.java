package be.geoffrey.xmlweave.core.usecase.choice;

import java.util.List;

public class ListChoice implements Choice {

    private List<String> possibleChoices;

    public ListChoice(List<String> possibleChoices) {
        this.possibleChoices = possibleChoices;
    }

    public List<String> getPossibleChoices() {
        return possibleChoices;
    }
}
