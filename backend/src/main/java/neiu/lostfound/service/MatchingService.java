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
    private final EmailNotificationService emailNotifications;

    public MatchingService(LostItemRepository lostRepo,
            FoundItemRepository foundRepo,
            ItemMatchRepository matchRepo,
            HandoffQueueRepository handoffRepo,
            EmailNotificationService emailNotifications) {
        this.lostRepo = lostRepo;
        this.foundRepo = foundRepo;
        this.matchRepo = matchRepo;
        this.handoffRepo = handoffRepo;
        this.emailNotifications = emailNotifications;
    }

    public List<LostItem> findMatchesForFound(Long foundId) {
        Optional<FoundItem> foundOpt = foundRepo.findById(foundId);
        if (foundOpt.isEmpty())
            return Collections.emptyList();
        FoundItem found = foundOpt.get();
        if (found.getKeywords() == null || found.getKeywords().isBlank())
            return Collections.emptyList();
        Set<String> foundKeywords = new HashSet<>(Arrays.asList(found.getKeywords().split(",")));

        // Filter by category AND subcategory - both must match
        // IMPORTANT: Exclude RETURNED items and items with confirmed matches (MATCHED
        // status)
        List<LostItem> allLost = lostRepo.findAll().stream()
                .filter(item -> {
                    // Exclude RETURNED items
                    if (item.getStatus() == LostItem.Status.RETURNED) {
                        return false;
                    }

                    // Exclude items that already have confirmed matches (MATCHED status)
                    if (item.getStatus() == LostItem.Status.MATCHED) {
                        return false;
                    }

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
            if (lost.getKeywords() == null || lost.getKeywords().isBlank())
                continue;
            List<ItemMatch> existingMatches = matchRepo.findByLostItemIdAndFoundItemId(lost.getId(), found.getId());
            boolean isConfirmed = existingMatches.stream().anyMatch(m -> m.getStatus() == ItemMatch.Status.CONFIRMED);
            if (isConfirmed)
                continue;

            Set<String> lostKeywords = new HashSet<>(Arrays.asList(lost.getKeywords().split(",")));
            Set<String> intersection = new HashSet<>(foundKeywords);
            intersection.retainAll(lostKeywords);
            int overlap = intersection.size();
            if (overlap > 0) {
                // Auto-create tentative match if not already present
                if (existingMatches.isEmpty()) {
                    ItemMatch tentative = new ItemMatch();
                    tentative.setLostItemId(lost.getId());
                    tentative.setFoundItemId(found.getId());
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
        if (lostOpt.isEmpty() || foundOpt.isEmpty())
            return null;

        LostItem lost = lostOpt.get();
        FoundItem found = foundOpt.get();

        // Check if this found item already has a confirmed match with a different lost
        // item
        Optional<ItemMatch> existingConfirmed = matchRepo.findByFoundItemIdAndStatus(foundId,
                ItemMatch.Status.CONFIRMED);
        if (existingConfirmed.isPresent() && !existingConfirmed.get().getLostItemId().equals(lostId)) {
            throw new IllegalStateException("Found item is already matched with another lost item");
        }

        // CRITICAL: Delete ALL other tentative matches for both this lost item and
        // found item
        List<ItemMatch> allMatchesForLost = matchRepo.findByLostItemId(lostId);
        List<ItemMatch> allMatchesForFound = matchRepo.findByFoundItemId(foundId);

        // Find the match we're confirming (if it exists as TENTATIVE)
        ItemMatch matchToConfirm = null;
        List<ItemMatch> matchesToDelete = new ArrayList<>();

        for (ItemMatch m : allMatchesForLost) {
            if (m.getLostItemId().equals(lostId) && m.getFoundItemId().equals(foundId)) {
                matchToConfirm = m; // This is the one we'll update
            } else {
                matchesToDelete.add(m); // Delete other matches for this lost item
            }
        }

        for (ItemMatch m : allMatchesForFound) {
            if (m.getLostItemId().equals(lostId) && m.getFoundItemId().equals(foundId)) {
                if (matchToConfirm == null) {
                    matchToConfirm = m; // Found it in the found item's matches
                }
            } else {
                if (!matchesToDelete.contains(m)) { // Avoid duplicates
                    matchesToDelete.add(m); // Delete other matches for this found item
                }
            }
        }

        // Delete all other matches
        if (!matchesToDelete.isEmpty()) {
            matchRepo.deleteAll(matchesToDelete);
        }

        // Update item statuses
        lost.setStatus(LostItem.Status.MATCHED);
        found.setStatus(FoundItem.Status.MATCHED);
        lostRepo.save(lost);
        foundRepo.save(found);

        // Update existing match OR create new one if doesn't exist
        ItemMatch match;
        if (matchToConfirm != null) {
            // Update the existing TENTATIVE match to CONFIRMED
            match = matchToConfirm;
            match.setStatus(ItemMatch.Status.CONFIRMED);
            match.setMatchedBy(adminUser);
            match.setMatchedAt(new Date());
        } else {
            // No existing match found, create new CONFIRMED match
            match = new ItemMatch();
            match.setLostItemId(lostId);
            match.setFoundItemId(foundId);
            match.setStatus(ItemMatch.Status.CONFIRMED);
            match.setMatchedBy(adminUser);
            match.setMatchedAt(new Date());
        }

        ItemMatch savedMatch = matchRepo.save(match);

        // Auto-create handoff when confirming match
        HandoffQueue handoff = new HandoffQueue();
        handoff.setMatchId(savedMatch.getId());
        handoff.setStatus(HandoffQueue.HandoffStatus.PENDING);
        handoff.setInitiatedBy(adminUser);
        handoff.setInitiatedAt(new Date());
        handoff.setNotes("Auto-created from confirmed match");
        HandoffQueue savedHandoff = handoffRepo.save(handoff);

        emailNotifications.sendMatchConfirmation(lost, found);
        emailNotifications.notifyHandoffCreated(savedHandoff);

        return savedMatch;
    }

    public ItemMatch createTentativeMatch(Long lostId, Long foundId, String adminUser) {
        Optional<LostItem> lostOpt = lostRepo.findById(lostId);
        Optional<FoundItem> foundOpt = foundRepo.findById(foundId);
        if (lostOpt.isEmpty() || foundOpt.isEmpty())
            return null;
        ItemMatch match = new ItemMatch();
        match.setLostItemId(lostId);
        match.setFoundItemId(foundId);
        match.setStatus(ItemMatch.Status.TENTATIVE);
        match.setMatchedBy(adminUser);
        match.setMatchedAt(new Date());
        return matchRepo.save(match);
    }

    public List<FoundItemWithMatches> getAllFoundItemsWithMatches() {
        // Show UNCLAIMED and MATCHED found items (exclude only RETURNED)
        List<FoundItem> allFoundItems = foundRepo.findAll().stream()
                .filter(item -> item.getStatus() != FoundItem.Status.RETURNED)
                .collect(Collectors.toList());

        List<FoundItemWithMatches> result = new ArrayList<>();

        for (FoundItem found : allFoundItems) {
            List<MatchResult> matchResults;
            int confirmed;
            int total;

            if (found.getStatus() == FoundItem.Status.MATCHED) {
                // Item is already matched - show only the confirmed match, no potential matches
                matchResults = getConfirmedMatchOnly(found);
                confirmed = matchResults.isEmpty() ? 0 : 1;
                total = 0; // No potential matches
            } else {
                // Item is UNCLAIMED - find potential matches
                matchResults = findMatchesWithConfidence(found);
                confirmed = (int) matchResults.stream().filter(MatchResult::isConfirmed).count();
                total = matchResults.size();
            }

            FoundItemWithMatches itemWithMatches = new FoundItemWithMatches();
            itemWithMatches.setFoundItem(found);
            itemWithMatches.setMatches(matchResults);
            itemWithMatches.setTotalMatches(total);
            itemWithMatches.setConfirmedMatches(confirmed);

            result.add(itemWithMatches);
        }

        return result;
    }

    private List<MatchResult> getConfirmedMatchOnly(FoundItem found) {
        // Find the confirmed match for this found item
        Optional<ItemMatch> confirmedMatch = matchRepo.findByFoundItemIdAndStatus(
                found.getId(),
                ItemMatch.Status.CONFIRMED);

        if (confirmedMatch.isEmpty()) {
            return Collections.emptyList();
        }

        ItemMatch match = confirmedMatch.get();
        Optional<LostItem> lostOpt = lostRepo.findById(match.getLostItemId());

        if (lostOpt.isEmpty()) {
            return Collections.emptyList();
        }

        LostItem lost = lostOpt.get();

        // Build match result with 100% confidence since it's confirmed
        String reason = "Confirmed match by " + match.getMatchedBy();
        MatchResult result = new MatchResult(lost, 100, reason, true);

        return Collections.singletonList(result);
    }

    private List<MatchResult> findMatchesWithConfidence(FoundItem found) {
        if (found.getKeywords() == null || found.getKeywords().isBlank()) {
            return Collections.emptyList();
        }

        Set<String> foundKeywords = new HashSet<>(Arrays.asList(found.getKeywords().split(",")));

        // Filter by category AND subcategory - both must match
        // IMPORTANT: Exclude RETURNED items and items with confirmed matches (MATCHED
        // status)
        List<LostItem> allLost = lostRepo.findAll().stream()
                .filter(item -> {
                    // Exclude RETURNED items
                    if (item.getStatus() == LostItem.Status.RETURNED) {
                        return false;
                    }

                    // Exclude items that already have confirmed matches (MATCHED status)
                    if (item.getStatus() == LostItem.Status.MATCHED) {
                        return false;
                    }

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
            if (lost.getKeywords() == null || lost.getKeywords().isBlank())
                continue;

            // Calculate confidence score
            int score = calculateConfidenceScore(found, lost, foundKeywords);
            if (score > 0) {
                // Check if already confirmed - handle multiple matches, pick CONFIRMED if
                // exists
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

        if (intersection.isEmpty())
            return 0;

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

                if (daysDiff == 0)
                    score += 10;
                else if (daysDiff <= 3)
                    score += 7;
                else if (daysDiff <= 7)
                    score += 5;
                else if (daysDiff <= 14)
                    score += 3;
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
