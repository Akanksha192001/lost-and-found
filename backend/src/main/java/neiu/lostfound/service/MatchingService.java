package neiu.lostfound.service;

import neiu.lostfound.dto.FoundItemWithMatches;
import neiu.lostfound.dto.MatchResult;
import neiu.lostfound.model.*;
import neiu.lostfound.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {
    private final LostItemRepository lostRepo;
    private final FoundItemRepository foundRepo;
    private final ItemMatchRepository matchRepo;
    private final HandoffQueueRepository handoffRepo;

    public MatchingService(LostItemRepository lostRepo, FoundItemRepository foundRepo, ItemMatchRepository matchRepo, HandoffQueueRepository handoffRepo) {
        this.lostRepo = lostRepo;
        this.foundRepo = foundRepo;
        this.matchRepo = matchRepo;
        this.handoffRepo = handoffRepo;
    }

    public List<LostItem> findMatchesForFound(Long foundId) {
        Optional<FoundItem> foundOpt = foundRepo.findById(foundId);
        if (foundOpt.isEmpty()) return Collections.emptyList();
        FoundItem found = foundOpt.get();
        if (found.getKeywords() == null || found.getKeywords().isBlank()) return Collections.emptyList();
        Set<String> foundKeywords = new HashSet<>(Arrays.asList(found.getKeywords().split(",")));
        
        // Filter by category AND subcategory - both must match
        List<LostItem> allLost = lostRepo.findAll().stream()
            .filter(item -> {
                // Category must match
                boolean categoryMatches = found.getCategory() != null && 
                    found.getCategory().equalsIgnoreCase(item.getCategory());
                
                // Subcategory must match
                boolean subcategoryMatches = found.getSubcategory() != null && 
                    found.getSubcategory().equalsIgnoreCase(item.getSubcategory());
                
                return categoryMatches && subcategoryMatches;
            })
            .collect(Collectors.toList());
        
        List<LostItem> matches = new ArrayList<>();
        int maxOverlap = 0;
        for (LostItem lost : allLost) {
            if (lost.getKeywords() == null || lost.getKeywords().isBlank()) continue;
            List<ItemMatch> existingMatches = matchRepo.findByLostItemIdAndFoundItemId(lost.getId(), found.getId());
            boolean isConfirmed = existingMatches.stream().anyMatch(m -> m.getStatus() == ItemMatch.Status.CONFIRMED);
            if (isConfirmed) continue;
            
            Set<String> lostKeywords = new HashSet<>(Arrays.asList(lost.getKeywords().split(",")));
            Set<String> intersection = new HashSet<>(foundKeywords);
            intersection.retainAll(lostKeywords);
            int overlap = intersection.size();
            if (overlap > 0) {
                // Auto-create tentative match if not already present
                if (existingMatches.isEmpty()) {
                    ItemMatch tentative = new ItemMatch();
                    tentative.setLostItem(lost);
                    tentative.setFoundItem(found);
                    tentative.setStatus(ItemMatch.Status.TENTATIVE);
                    tentative.setMatchedBy("system");
                    tentative.setMatchedAt(new Date());
                    matchRepo.save(tentative);
                }
                if (overlap > maxOverlap) {
                    matches.clear();
                    matches.add(lost);
                    maxOverlap = overlap;
                } else if (overlap == maxOverlap) {
                    matches.add(lost);
                }
            }
        }
        return matches;
    }

    public ItemMatch confirmMatch(Long lostId, Long foundId, String adminUser) {
        Optional<LostItem> lostOpt = lostRepo.findById(lostId);
        Optional<FoundItem> foundOpt = foundRepo.findById(foundId);
        if (lostOpt.isEmpty() || foundOpt.isEmpty()) return null;
        
        LostItem lost = lostOpt.get();
        FoundItem found = foundOpt.get();
        
        // Check if this found item already has a confirmed match with a different lost item
        Optional<ItemMatch> existingConfirmed = matchRepo.findByFoundItemIdAndStatus(foundId, ItemMatch.Status.CONFIRMED);
        if (existingConfirmed.isPresent() && !existingConfirmed.get().getLostItem().getId().equals(lostId)) {
            throw new IllegalStateException("Found item is already matched with another lost item");
        }
        
        // Update item statuses
        lost.setMatchedWith(found);
        lost.setStatus(LostItem.Status.MATCHED);
        found.setMatchedWith(lost);
        found.setStatus(FoundItem.Status.MATCHED);
        lostRepo.save(lost);
        foundRepo.save(found);
        
        // Create or update match record
        ItemMatch match = new ItemMatch();
        match.setLostItem(lost);
        match.setFoundItem(found);
        match.setStatus(ItemMatch.Status.CONFIRMED);
        match.setMatchedBy(adminUser);
        match.setMatchedAt(new Date());
        ItemMatch savedMatch = matchRepo.save(match);
        
        // Auto-create handoff when confirming match
        HandoffQueue handoff = new HandoffQueue();
        handoff.setLostItem(lost);
        handoff.setFoundItem(found);
        handoff.setItemMatch(savedMatch);
        handoff.setStatus(HandoffQueue.HandoffStatus.PENDING);
        handoff.setInitiatedBy(adminUser);
        handoff.setInitiatedAt(new Date());
        handoff.setNotes("Auto-created from confirmed match");
        handoffRepo.save(handoff);
        
        return savedMatch;
    }

    public ItemMatch createTentativeMatch(Long lostId, Long foundId, String adminUser) {
        Optional<LostItem> lostOpt = lostRepo.findById(lostId);
        Optional<FoundItem> foundOpt = foundRepo.findById(foundId);
        if (lostOpt.isEmpty() || foundOpt.isEmpty()) return null;
        ItemMatch match = new ItemMatch();
        match.setLostItem(lostOpt.get());
        match.setFoundItem(foundOpt.get());
        match.setStatus(ItemMatch.Status.TENTATIVE);
        match.setMatchedBy(adminUser);
        match.setMatchedAt(new Date());
        return matchRepo.save(match);
    }

    public List<FoundItemWithMatches> getAllFoundItemsWithMatches() {
        List<FoundItem> allFoundItems = foundRepo.findAll();
        List<FoundItemWithMatches> result = new ArrayList<>();

        for (FoundItem found : allFoundItems) {
            List<MatchResult> matchResults = findMatchesWithConfidence(found);
            int confirmed = (int) matchResults.stream().filter(MatchResult::isConfirmed).count();
            
            FoundItemWithMatches itemWithMatches = new FoundItemWithMatches();
            itemWithMatches.setFoundItem(found);
            itemWithMatches.setMatches(matchResults);
            itemWithMatches.setTotalMatches(matchResults.size());
            itemWithMatches.setConfirmedMatches(confirmed);
            
            result.add(itemWithMatches);
        }

        return result;
    }

    private List<MatchResult> findMatchesWithConfidence(FoundItem found) {
        if (found.getKeywords() == null || found.getKeywords().isBlank()) {
            return Collections.emptyList();
        }

        Set<String> foundKeywords = new HashSet<>(Arrays.asList(found.getKeywords().split(",")));
        
        // Filter by category AND subcategory - both must match
        List<LostItem> allLost = lostRepo.findAll().stream()
            .filter(item -> {
                // Category must match
                boolean categoryMatches = found.getCategory() != null && 
                    found.getCategory().equalsIgnoreCase(item.getCategory());
                
                // Subcategory must match
                boolean subcategoryMatches = found.getSubcategory() != null && 
                    found.getSubcategory().equalsIgnoreCase(item.getSubcategory());
                
                return categoryMatches && subcategoryMatches;
            })
            .collect(Collectors.toList());

        List<MatchResult> results = new ArrayList<>();

        for (LostItem lost : allLost) {
            if (lost.getKeywords() == null || lost.getKeywords().isBlank()) continue;

            // Calculate confidence score
            int score = calculateConfidenceScore(found, lost, foundKeywords);
            if (score > 0) {
                // Check if already confirmed - handle multiple matches, pick CONFIRMED if exists
                List<ItemMatch> existingMatches = matchRepo.findByLostItemIdAndFoundItemId(lost.getId(), found.getId());
                boolean isConfirmed = existingMatches.stream()
                    .anyMatch(m -> m.getStatus() == ItemMatch.Status.CONFIRMED);

                String reason = buildMatchReason(found, lost, foundKeywords);
                results.add(new MatchResult(lost, score, reason, isConfirmed));
            }
        }

        // Sort by confidence score (highest first)
        results.sort((a, b) -> Integer.compare(b.getConfidenceScore(), a.getConfidenceScore()));
        return results;
    }

    private int calculateConfidenceScore(FoundItem found, LostItem lost, Set<String> foundKeywords) {
        Set<String> lostKeywords = new HashSet<>(Arrays.asList(lost.getKeywords().split(",")));
        Set<String> intersection = new HashSet<>(foundKeywords);
        intersection.retainAll(lostKeywords);

        if (intersection.isEmpty()) return 0;

        int score = 0;

        // Keyword overlap (0-60 points)
        double keywordRatio = (double) intersection.size() / Math.max(foundKeywords.size(), lostKeywords.size());
        score += (int) (keywordRatio * 60);

        // Category match (20 points)
        if (found.getCategory() != null && lost.getCategory() != null && 
            found.getCategory().equalsIgnoreCase(lost.getCategory())) {
            score += 20;
        }

        // Subcategory match (10 points)
        if (found.getSubcategory() != null && lost.getSubcategory() != null && 
            found.getSubcategory().equalsIgnoreCase(lost.getSubcategory())) {
            score += 10;
        }

        // Date proximity (0-10 points) - closer dates = higher score
        if (found.getDateFound() != null && lost.getDateLost() != null) {
            try {
                LocalDate foundDate = found.getDateFound().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate lostDate = lost.getDateLost().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                long daysDiff = Math.abs(ChronoUnit.DAYS.between(foundDate, lostDate));
                
                if (daysDiff == 0) score += 10;
                else if (daysDiff <= 3) score += 7;
                else if (daysDiff <= 7) score += 5;
                else if (daysDiff <= 14) score += 3;
            } catch (UnsupportedOperationException e) {
                // Skip date scoring if conversion fails (e.g., java.sql.Date)
            }
        }

        return Math.min(score, 100); // Cap at 100
    }

    private String buildMatchReason(FoundItem found, LostItem lost, Set<String> foundKeywords) {
        Set<String> lostKeywords = new HashSet<>(Arrays.asList(lost.getKeywords().split(",")));
        Set<String> intersection = new HashSet<>(foundKeywords);
        intersection.retainAll(lostKeywords);

        List<String> reasons = new ArrayList<>();
        reasons.add(intersection.size() + " matching keywords: " + String.join(", ", intersection));
        
        if (found.getCategory() != null && lost.getCategory() != null && 
            found.getCategory().equalsIgnoreCase(lost.getCategory())) {
            reasons.add("Same category: " + found.getCategory());
        }

        if (found.getSubcategory() != null && lost.getSubcategory() != null && 
            found.getSubcategory().equalsIgnoreCase(lost.getSubcategory())) {
            reasons.add("Same subcategory: " + found.getSubcategory());
        }

        return String.join(" | ", reasons);
    }
}
