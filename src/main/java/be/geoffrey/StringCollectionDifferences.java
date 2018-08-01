package be.geoffrey;

import java.util.*;

public class StringCollectionDifferences {

    private Set<String> removedItems = new HashSet<>();
    private Set<String> newItems = new HashSet<>();
    private Set<String> common = new HashSet<>();
    private Map<String, String> caseChanges = new HashMap<>();

    public StringCollectionDifferences(Collection<String> originalNames, Collection<String> newNames) {

        newNames.forEach(newName -> {
            for (String originalName : originalNames) {
                if (newName.equalsIgnoreCase(originalName) && !newName.equals(originalName)) {
                    caseChanges.put(originalName, newName);
                }
            }
        });

        for (String originalName : originalNames) {
            if (!newNames.contains(originalName) && !caseChanges.containsKey(originalName)) {
                removedItems.add(originalName);
            }
        }

        for (String newName : newNames) {

            if (originalNames.contains(newName)) {
                common.add(newName);
            } else if (!caseChanges.containsValue(newName)) {
                newItems.add(newName);
            }
        }
    }

    public Set<String> getRemovedItems() {
        return removedItems;
    }

    public Set<String> getNewItems() {
        return newItems;
    }

    public Set<String> getCommon() {
        return common;
    }

    public Map<String, String> getCaseChanges() {
        return caseChanges;
    }
}