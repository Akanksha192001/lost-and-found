package neiu.lostfound.dto;

import neiu.lostfound.model.LostItem;

public class MatchResult {
    private LostItem lostItem;
    private int confidenceScore; // 0-100
    private String matchReason;
    private boolean isConfirmed;

    public MatchResult() {}

    public MatchResult(LostItem lostItem, int confidenceScore, String matchReason, boolean isConfirmed) {
        this.lostItem = lostItem;
        this.confidenceScore = confidenceScore;
        this.matchReason = matchReason;
        this.isConfirmed = isConfirmed;
    }

    // Getters and setters
    public LostItem getLostItem() {
        return lostItem;
    }

    public void setLostItem(LostItem lostItem) {
        this.lostItem = lostItem;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(int confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public void setConfirmed(boolean confirmed) {
        isConfirmed = confirmed;
    }
}
