package neiu.lostfound.dto;

import neiu.lostfound.model.LostItem;

public class MatchResult {
    private LostItem lostItem;
    private int confidenceScore; // 0-100 (text-based or AI-based depending on availability)
    private String matchReason; // Text-based reason or AI reason
    private boolean isConfirmed;
    
    // AI-specific matching results (only populated when AI is enabled)
    private Integer aiConfidenceScore; // AI confidence score (0-100)
    private String aiReasoning; // Natural language AI reasoning
    private java.util.List<String> aiMatchingFeatures; // Features that match according to AI
    private java.util.List<String> aiDiscrepancies; // Discrepancies found by AI
    
    // Text-based matching results (always populated as fallback)
    private Integer textConfidenceScore; // Keyword-based confidence score
    private String textMatchReason; // Keyword-based reasoning

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

    public Integer getAiConfidenceScore() {
        return aiConfidenceScore;
    }

    public void setAiConfidenceScore(Integer aiConfidenceScore) {
        this.aiConfidenceScore = aiConfidenceScore;
    }

    public String getAiReasoning() {
        return aiReasoning;
    }

    public void setAiReasoning(String aiReasoning) {
        this.aiReasoning = aiReasoning;
    }

    public java.util.List<String> getAiMatchingFeatures() {
        return aiMatchingFeatures;
    }

    public void setAiMatchingFeatures(java.util.List<String> aiMatchingFeatures) {
        this.aiMatchingFeatures = aiMatchingFeatures;
    }

    public java.util.List<String> getAiDiscrepancies() {
        return aiDiscrepancies;
    }

    public void setAiDiscrepancies(java.util.List<String> aiDiscrepancies) {
        this.aiDiscrepancies = aiDiscrepancies;
    }

    public Integer getTextConfidenceScore() {
        return textConfidenceScore;
    }

    public void setTextConfidenceScore(Integer textConfidenceScore) {
        this.textConfidenceScore = textConfidenceScore;
    }

    public String getTextMatchReason() {
        return textMatchReason;
    }

    public void setTextMatchReason(String textMatchReason) {
        this.textMatchReason = textMatchReason;
    }
}
