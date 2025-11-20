package neiu.lostfound.dto;

import neiu.lostfound.model.FoundItem;
import java.util.List;

public class FoundItemWithMatches {
    private FoundItem foundItem;
    private List<MatchResult> matches;
    private int totalMatches;
    private int confirmedMatches;

    public FoundItemWithMatches() {}

    public FoundItemWithMatches(FoundItem foundItem, List<MatchResult> matches, int totalMatches, int confirmedMatches) {
        this.foundItem = foundItem;
        this.matches = matches;
        this.totalMatches = totalMatches;
        this.confirmedMatches = confirmedMatches;
    }

    // Getters and setters
    public FoundItem getFoundItem() {
        return foundItem;
    }

    public void setFoundItem(FoundItem foundItem) {
        this.foundItem = foundItem;
    }

    public List<MatchResult> getMatches() {
        return matches;
    }

    public void setMatches(List<MatchResult> matches) {
        this.matches = matches;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }

    public int getConfirmedMatches() {
        return confirmedMatches;
    }

    public void setConfirmedMatches(int confirmedMatches) {
        this.confirmedMatches = confirmedMatches;
    }
}
