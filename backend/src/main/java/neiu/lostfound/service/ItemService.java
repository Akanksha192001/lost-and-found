package neiu.lostfound.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import neiu.lostfound.dto.FoundItemRequest;
import neiu.lostfound.dto.LostItemRequest;
import neiu.lostfound.model.LostItem;
import neiu.lostfound.model.FoundItem;
import neiu.lostfound.repository.LostItemRepository;
import neiu.lostfound.repository.FoundItemRepository;
import neiu.lostfound.repository.UserRepository;
import neiu.lostfound.config.StopWordsProvider;
import neiu.lostfound.service.KeywordProcessor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
  private static final Logger log = LoggerFactory.getLogger(ItemService.class);
  private final LostItemRepository lostRepo;
  private final FoundItemRepository foundRepo;
  private final UserRepository userRepo;
  private final StopWordsProvider stopWordsProvider;
  private final KeywordProcessor keywordProcessor;

  @Autowired
  public ItemService(LostItemRepository lostRepo, FoundItemRepository foundRepo, UserRepository userRepo, StopWordsProvider stopWordsProvider, KeywordProcessor keywordProcessor) {
    this.lostRepo = lostRepo;
    this.foundRepo = foundRepo;
    this.userRepo = userRepo;
    this.stopWordsProvider = stopWordsProvider;
    this.keywordProcessor = keywordProcessor;
  }

  private String extractKeywords(String... fields) {
    Set<String> keywords = keywordProcessor.process(fields);
    return String.join(",", keywords);
  }

  public LostItem createLost(LostItemRequest req, String userEmail) {
    log.info("Creating lost item: title={}, userEmail={}", req.title, userEmail);
    LostItem it = new LostItem();
    it.setTitle(req.title == null ? null : req.title.trim());
    it.setDescription(req.description);
    it.setLocation(req.location == null ? null : req.location.trim());
    try {
      if (req.dateLost != null) {
        it.setDateLost(new SimpleDateFormat("yyyy-MM-dd").parse(req.dateLost.trim()));
      }
    } catch (Exception e) { log.warn("Invalid dateLost format"); }
    it.setImageData(req.imageData);
    // New lost items are OPEN by default
    it.setStatus(LostItem.Status.OPEN);
    if (userEmail != null) {
      userRepo.findByEmail(userEmail).ifPresent(user -> {
        it.setReportedBy(user);
        // Set owner details - use provided values if available (ADMIN case), otherwise use authenticated user
        if (req.ownerName != null && !req.ownerName.trim().isEmpty()) {
          it.setOwnerName(req.ownerName.trim());
        } else {
          it.setOwnerName(user.getName());
        }
        if (req.ownerEmail != null && !req.ownerEmail.trim().isEmpty()) {
          it.setOwnerEmail(req.ownerEmail.trim());
        } else {
          it.setOwnerEmail(user.getEmail());
        }
      });
    }
    if (req.matchedWith != null) {
      foundRepo.findById(req.matchedWith).ifPresent(it::setMatchedWith);
    }
    // Extract and set keywords
    it.setKeywords(extractKeywords(req.title, req.description, req.location, req.ownerName));
    it.setCategory(req.category);
    it.setSubcategory(req.subcategory);
    return lostRepo.save(it);
  }

  public FoundItem createFound(FoundItemRequest req, String userEmail) {
    log.info("Creating found item: title={}, userEmail={}", req.title, userEmail);
    FoundItem it = new FoundItem();
    it.setTitle(req.title == null ? null : req.title.trim());
    it.setDescription(req.description);
    it.setLocation(req.location == null ? null : req.location.trim());
    try {
      if (req.dateFound != null) {
        it.setDateFound(new SimpleDateFormat("yyyy-MM-dd").parse(req.dateFound.trim()));
      }
    } catch (Exception e) { log.warn("Invalid dateFound format"); }
    it.setImageData(req.imageData);
    // New found items are UNCLAIMED by default
    it.setStatus(FoundItem.Status.UNCLAIMED);
    if (userEmail != null) {
      userRepo.findByEmail(userEmail).ifPresent(user -> {
        it.setReportedBy(user);
        // Set reporter details - use provided values if available (ADMIN case), otherwise use authenticated user
        if (req.reporterName != null && !req.reporterName.trim().isEmpty()) {
          it.setReporterName(req.reporterName.trim());
        } else {
          it.setReporterName(user.getName());
        }
        if (req.reporterEmail != null && !req.reporterEmail.trim().isEmpty()) {
          it.setReporterEmail(req.reporterEmail.trim());
        } else {
          it.setReporterEmail(user.getEmail());
        }
      });
    }
    if (req.matchedWith != null) {
      lostRepo.findById(req.matchedWith).ifPresent(it::setMatchedWith);
    }
    // Extract and set keywords
    it.setKeywords(extractKeywords(req.title, req.description, req.location, req.reporterName));
    it.setCategory(req.category);
    it.setSubcategory(req.subcategory);
    return foundRepo.save(it);
  }

  public List<LostItem> listLost() {
    return lostRepo.findAll();
  }
  
  public List<LostItem> listLostByUser(String userEmail) {
    log.info("Fetching lost items for user: {}", userEmail);
    return lostRepo.findByReportedByEmail(userEmail);
  }
  
  public List<FoundItem> listFound() {
    return foundRepo.findAll();
  }

  public List<LostItem> searchLostItems(String q, String category, String subcategory) {
    List<LostItem> all = lostRepo.findAll();
    List<LostItem> filtered = all.stream()
      .filter(item -> matchCategory(item, category, subcategory))
      .collect(Collectors.toList());
    if (q == null || q.isBlank()) {
      // No keyword filtering, just return all matches
      return filtered;
    }
    Set<String> queryKeywords = Arrays.stream(q.split(","))
      .map(String::trim)
      .map(String::toLowerCase)
      .filter(s -> !s.isEmpty())
      .collect(Collectors.toSet());
    List<LostItem> ranked = filtered.stream()
      .map(item -> new AbstractMap.SimpleEntry<>(item, countMatches(queryKeywords, item.getKeywords())))
      .filter(e -> e.getValue() > 0)
      .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
    return ranked;
  }

  public List<FoundItem> searchFoundItems(String q, String category, String subcategory) {
    List<FoundItem> all = foundRepo.findAll();
    List<FoundItem> filtered = all.stream()
      .filter(item -> matchCategory(item, category, subcategory))
      .collect(Collectors.toList());
    if (q == null || q.isBlank()) {
      // No keyword filtering, just return all matches
      return filtered;
    }
    Set<String> queryKeywords = Arrays.stream(q.split(","))
      .map(String::trim)
      .map(String::toLowerCase)
      .filter(s -> !s.isEmpty())
      .collect(Collectors.toSet());
    List<FoundItem> ranked = filtered.stream()
      .map(item -> new AbstractMap.SimpleEntry<>(item, countMatches(queryKeywords, item.getKeywords())))
      .filter(e -> e.getValue() > 0)
      .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
    return ranked;
  }

  private boolean matchCategory(Object item, String category, String subcategory) {
    String itemCat = null;
    String itemSubcat = null;
    if (item instanceof LostItem) {
      itemCat = ((LostItem)item).getCategory();
      itemSubcat = ((LostItem)item).getSubcategory();
    } else if (item instanceof FoundItem) {
      itemCat = ((FoundItem)item).getCategory();
      itemSubcat = ((FoundItem)item).getSubcategory();
    }
    if (subcategory != null && !subcategory.isBlank()) {
      return subcategory.equalsIgnoreCase(itemSubcat);
    } else if (category != null && !category.isBlank()) {
      return category.equalsIgnoreCase(itemCat);
    }
    return true;
  }

  private int countMatches(Set<String> queryKeywords, String itemKeywords) {
    if (itemKeywords == null || itemKeywords.isBlank()) return 0;
    Set<String> itemSet = Arrays.stream(itemKeywords.split(","))
      .map(String::trim)
      .map(String::toLowerCase)
      .filter(s -> !s.isEmpty())
      .collect(Collectors.toSet());
    int count = 0;
    for (String qk : queryKeywords) {
      if (itemSet.contains(qk)) count++;
    }
    return count;
  }
}
