package neiu.lostfound.dto;

import java.util.List;

public class AIAnalysisResponse {
    private int confidenceScore;
    private String reasoning;
    private List<String> matchingFeatures;
    private List<String> discrepancies;

    public AIAnalysisResponse() {}

    public AIAnalysisResponse(int confidenceScore, String reasoning, 
                             List<String> matchingFeatures, List<String> discrepancies) {
        this.confidenceScore = confidenceScore;
        this.reasoning = reasoning;
        this.matchingFeatures = matchingFeatures;
        this.discrepancies = discrepancies;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(int confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<String> getMatchingFeatures() {
        return matchingFeatures;
    }

    public void setMatchingFeatures(List<String> matchingFeatures) {
        this.matchingFeatures = matchingFeatures;
    }

    public List<String> getDiscrepancies() {
        return discrepancies;
    }

    public void setDiscrepancies(List<String> discrepancies) {
        this.discrepancies = discrepancies;
    }
}
