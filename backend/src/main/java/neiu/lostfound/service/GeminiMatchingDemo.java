package neiu.lostfound.service;

import neiu.lostfound.model.FoundItem;
import neiu.lostfound.model.LostItem;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Example demonstrating Gemini AI matching functionality.
 * This component only runs when app.gemini.demo=true is set.
 * 
 * To run this demo:
 * 1. Set environment variable: GEMINI_ENABLED=true
 * 2. Set environment variable: GEMINI_API_KEY=your_api_key
 * 3. Add to application.properties: app.gemini.demo=true
 * 4. Run the application
 */
@Component
@ConditionalOnProperty(name = "app.gemini.demo", havingValue = "true")
public class GeminiMatchingDemo implements CommandLineRunner {
    
    private final GeminiMatchingService geminiMatchingService;
    
    public GeminiMatchingDemo(GeminiMatchingService geminiMatchingService) {
        this.geminiMatchingService = geminiMatchingService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        if (!geminiMatchingService.isEnabled()) {
            System.out.println("===================================");
            System.out.println("Gemini AI Matching Demo");
            System.out.println("===================================");
            System.out.println("ERROR: Gemini AI is not enabled!");
            System.out.println("Please set:");
            System.out.println("  GEMINI_ENABLED=true");
            System.out.println("  GEMINI_API_KEY=your_api_key");
            System.out.println("===================================");
            return;
        }
        
        System.out.println("\n===================================");
        System.out.println("Gemini AI Matching Demo");
        System.out.println("===================================\n");
        
        // Example 1: Clear match
        demonstrateMatch(
            "Blue iPhone 13 with cracked screen",
            "Blue iPhone with broken screen and black case",
            "Electronics",
            "Phone"
        );
        
        // Example 2: Partial match
        demonstrateMatch(
            "Black leather wallet with driver's license",
            "Brown leather wallet with ID cards",
            "Personal Items",
            "Wallet"
        );
        
        // Example 3: No match
        demonstrateMatch(
            "Red Nike backpack with laptop",
            "Blue Adidas water bottle",
            "Accessories",
            "Bag"
        );
        
        System.out.println("===================================");
        System.out.println("Demo completed!");
        System.out.println("===================================\n");
    }
    
    private void demonstrateMatch(String foundDesc, String lostDesc, String category, String subcategory) {
        System.out.println("Testing Match:");
        System.out.println("  Found: " + foundDesc);
        System.out.println("  Lost:  " + lostDesc);
        System.out.println();
        
        // Create test items
        FoundItem found = new FoundItem();
        found.setId(1L);
        found.setDescription(foundDesc);
        found.setCategory(category);
        found.setSubcategory(subcategory);
        found.setDateFound(new Date());
        found.setLocation("Library");
        found.setKeywords(extractSimpleKeywords(foundDesc));
        
        LostItem lost = new LostItem();
        lost.setId(1L);
        lost.setDescription(lostDesc);
        lost.setCategory(category);
        lost.setSubcategory(subcategory);
        lost.setDateLost(new Date());
        lost.setLocation("Campus Center");
        lost.setKeywords(extractSimpleKeywords(lostDesc));
        
        // Get AI match result
        try {
            GeminiMatchingService.GeminiMatchResult result = 
                geminiMatchingService.calculateMatchConfidence(lost, found);
            
            if (result != null) {
                System.out.println("  Confidence Score: " + result.getConfidenceScore() + "/100");
                System.out.println("  Reasoning: " + result.getReasoning());
                
                if (!result.getMatchingFeatures().isEmpty()) {
                    System.out.println("  Matching Features:");
                    result.getMatchingFeatures().forEach(f -> 
                        System.out.println("    - " + f));
                }
                
                if (!result.getDiscrepancies().isEmpty()) {
                    System.out.println("  Discrepancies:");
                    result.getDiscrepancies().forEach(d -> 
                        System.out.println("    - " + d));
                }
            } else {
                System.out.println("  ERROR: No result returned from AI");
            }
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private String extractSimpleKeywords(String description) {
        // Simple keyword extraction for demo purposes
        return description.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", ",");
    }
}
