package neiu.lostfound.service;

import neiu.lostfound.model.*;
import neiu.lostfound.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MatchingService {
    private final LostItemRepository lostRepo;
    private final FoundItemRepository foundRepo;
    private final ItemMatchRepository matchRepo;
    private final ReturnedItemRepository returnedRepo;

    public MatchingService(LostItemRepository lostRepo, FoundItemRepository foundRepo, ItemMatchRepository matchRepo, ReturnedItemRepository returnedRepo) {
        this.lostRepo = lostRepo;
        this.foundRepo = foundRepo;
        this.matchRepo = matchRepo;
        this.returnedRepo = returnedRepo;
    }

    public List<FoundItem> findMatchesForLost(Long lostId) {
        Optional<LostItem> lostOpt = lostRepo.findById(lostId);
        if (lostOpt.isEmpty()) return Collections.emptyList();
        LostItem lost = lostOpt.get();
        if (lost.getKeywords() == null || lost.getKeywords().isBlank()) return Collections.emptyList();
        Set<String> lostKeywords = new HashSet<>(Arrays.asList(lost.getKeywords().split(",")));
        List<FoundItem> allFound = foundRepo.findAll();
        List<FoundItem> matches = new ArrayList<>();
        int maxOverlap = 0;
        for (FoundItem found : allFound) {
            if (found.getKeywords() == null || found.getKeywords().isBlank()) continue;
            Optional<ItemMatch> existingMatch = matchRepo.findByLostItemIdAndFoundItemId(lost.getId(), found.getId());
            if (existingMatch.isPresent() && existingMatch.get().getStatus() == ItemMatch.Status.CONFIRMED) continue;
            Set<String> foundKeywords = new HashSet<>(Arrays.asList(found.getKeywords().split(",")));
            Set<String> intersection = new HashSet<>(lostKeywords);
            intersection.retainAll(foundKeywords);
            int overlap = intersection.size();
            if (overlap > 0) {
                // Auto-create tentative match if not already present
                if (existingMatch.isEmpty()) {
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
                    matches.add(found);
                    maxOverlap = overlap;
                } else if (overlap == maxOverlap) {
                    matches.add(found);
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
        lost.setMatchedWith(found);
        lost.setStatus(LostItem.Status.MATCHED);
        found.setMatchedWith(lost);
        found.setStatus(FoundItem.Status.MATCHED);
        lostRepo.save(lost);
        foundRepo.save(found);
        ItemMatch match = new ItemMatch();
        match.setLostItem(lost);
        match.setFoundItem(found);
        match.setStatus(ItemMatch.Status.CONFIRMED);
        match.setMatchedBy(adminUser);
        match.setMatchedAt(new Date());
        return matchRepo.save(match);
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

    public void returnItem(Long lostId, Long foundId) {
        Optional<LostItem> lostOpt = lostRepo.findById(lostId);
        Optional<FoundItem> foundOpt = foundRepo.findById(foundId);
        if (lostOpt.isPresent()) {
            LostItem lost = lostOpt.get();
            ReturnedItem returned = new ReturnedItem();
            returned.setTitle(lost.getTitle());
            returned.setDescription(lost.getDescription());
            returned.setLocation(lost.getLocation());
            returned.setDateReturned(new Date());
            returned.setImageUrl(lost.getImageUrl());
            returned.setOwnerName(lost.getOwnerName());
            returned.setOwnerEmail(lost.getOwnerEmail());
            returned.setOwnerAddress(lost.getOwnerAddress());
            returnedRepo.save(returned);
            lostRepo.delete(lost);
        }
        if (foundOpt.isPresent()) {
            FoundItem found = foundOpt.get();
            ReturnedItem returned = new ReturnedItem();
            returned.setTitle(found.getTitle());
            returned.setDescription(found.getDescription());
            returned.setLocation(found.getLocation());
            returned.setDateReturned(new Date());
            returned.setImageUrl(found.getImageUrl());
            returned.setReporterName(found.getReporterName());
            returned.setReporterEmail(found.getReporterEmail());
            returned.setReporterAddress(found.getReporterAddress());
            returnedRepo.save(returned);
            foundRepo.delete(found);
        }
    }
}
