package neiu.lostfound.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import neiu.lostfound.model.FoundItem;
import neiu.lostfound.model.LostItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiMatchingService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiMatchingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${app.gemini.enabled:false}")
    private boolean geminiEnabled;
    
    @Value("${app.gemini.api-key:}")
    private String apiKey;
    
    @Value("${app.gemini.model:gemini-2.5-flash}")
    private String model;
    
    private Client client;
    private String systemInstruction;
    
    public boolean isEnabled() {
        return geminiEnabled && apiKey != null && !apiKey.isBlank();
    }
    
    private void initializeClient() {
        if (client == null && isEnabled()) {
            try {
                // Initialize client with API key using builder pattern
                client = Client.builder().apiKey(apiKey).build();
                loadSystemInstruction();
                logger.info("Gemini AI client initialized successfully with model: {}", model);
            } catch (Exception e) {
                logger.error("Failed to initialize Gemini AI client", e);
                geminiEnabled = false;
            }
        }
    }
    
    private void loadSystemInstruction() {
        try {
            ClassPathResource resource = new ClassPathResource("gemini-matching-instructions.txt");
            systemInstruction = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            logger.info("Loaded Gemini system instructions successfully");
        } catch (IOException e) {
            logger.error("Failed to load system instructions file", e);
            systemInstruction = "You are an AI assistant that matches lost and found items. Respond with JSON containing confidenceScore (0-100) and reasoning.";
        }
    }
    
    /**
     * Use Gemini AI to calculate match confidence between lost and found items
     */
    public GeminiMatchResult calculateMatchConfidence(LostItem lost, FoundItem found) {
        if (!isEnabled()) {
            return null;
        }
        
        initializeClient();
        
        if (client == null) {
            return null;
        }
        
        try {
            String prompt = buildMatchingPrompt(lost, found);
            
            // Create content with system instruction
            List<Content> contents = new ArrayList<>();
            contents.add(Content.builder().role("user").parts(List.of(Part.fromText(systemInstruction))).build());
            contents.add(Content.builder().role("user").parts(List.of(Part.fromText(prompt))).build());
            
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.3f)  // Lower temperature for more consistent results
                    .build();
            
            GenerateContentResponse response = client.models.generateContent(
                    model,
                    contents,
                    config
            );
            
            String responseText = response.text();
            logger.debug("Gemini AI response: {}", responseText);
            
            return parseResponse(responseText);
            
        } catch (Exception e) {
            logger.error("Error calling Gemini AI for matching", e);
            return null;
        }
    }
    
    private String buildMatchingPrompt(LostItem lost, FoundItem found) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze if these items match. Both items are in the same category (")
              .append(found.getCategory()).append(") and subcategory (")
              .append(found.getSubcategory()).append("), so focus on comparing their descriptions.\n\n");
        
        prompt.append("FOUND ITEM:\n");
        prompt.append("- Description: ").append(found.getDescription()).append("\n");
        if (found.getDateFound() != null) {
            prompt.append("- Date Found: ").append(dateFormat.format(found.getDateFound())).append("\n");
        }
        if (found.getLocation() != null) {
            prompt.append("- Location Found: ").append(found.getLocation()).append("\n");
        }
        if (found.getKeywords() != null && !found.getKeywords().isBlank()) {
            prompt.append("- Keywords: ").append(found.getKeywords().replace(",", ", ")).append("\n");
        }
        
        prompt.append("\nLOST ITEM:\n");
        prompt.append("- Description: ").append(lost.getDescription()).append("\n");
        if (lost.getDateLost() != null) {
            prompt.append("- Date Lost: ").append(dateFormat.format(lost.getDateLost())).append("\n");
        }
        if (lost.getLocation() != null) {
            prompt.append("- Location Lost: ").append(lost.getLocation()).append("\n");
        }
        if (lost.getKeywords() != null && !lost.getKeywords().isBlank()) {
            prompt.append("- Keywords: ").append(lost.getKeywords().replace(",", ", ")).append("\n");
        }
        
        prompt.append("\nProvide your analysis in JSON format.");
        
        return prompt.toString();
    }
    
    private GeminiMatchResult parseResponse(String responseText) {
        try {
            // Extract JSON from response (may be wrapped in markdown code blocks)
            String jsonText = responseText.trim();
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            } else if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            jsonText = jsonText.trim();
            
            JsonNode root = objectMapper.readTree(jsonText);
            
            int confidenceScore = root.has("confidenceScore") ? root.get("confidenceScore").asInt() : 0;
            String reasoning = root.has("reasoning") ? root.get("reasoning").asText() : "No reasoning provided";
            
            List<String> matchingFeatures = new ArrayList<>();
            if (root.has("matchingFeatures") && root.get("matchingFeatures").isArray()) {
                root.get("matchingFeatures").forEach(node -> matchingFeatures.add(node.asText()));
            }
            
            List<String> discrepancies = new ArrayList<>();
            if (root.has("discrepancies") && root.get("discrepancies").isArray()) {
                root.get("discrepancies").forEach(node -> discrepancies.add(node.asText()));
            }
            
            return new GeminiMatchResult(confidenceScore, reasoning, matchingFeatures, discrepancies);
            
        } catch (Exception e) {
            logger.error("Failed to parse Gemini AI response: {}", responseText, e);
            return new GeminiMatchResult(0, "Failed to parse AI response", new ArrayList<>(), new ArrayList<>());
        }
    }
    
    /**
     * Result object from Gemini AI matching
     */
    public static class GeminiMatchResult {
        private final int confidenceScore;
        private final String reasoning;
        private final List<String> matchingFeatures;
        private final List<String> discrepancies;
        
        public GeminiMatchResult(int confidenceScore, String reasoning, 
                                List<String> matchingFeatures, List<String> discrepancies) {
            this.confidenceScore = confidenceScore;
            this.reasoning = reasoning;
            this.matchingFeatures = matchingFeatures;
            this.discrepancies = discrepancies;
        }
        
        public int getConfidenceScore() {
            return confidenceScore;
        }
        
        public String getReasoning() {
            return reasoning;
        }
        
        public List<String> getMatchingFeatures() {
            return matchingFeatures;
        }
        
        public List<String> getDiscrepancies() {
            return discrepancies;
        }
    }
}
