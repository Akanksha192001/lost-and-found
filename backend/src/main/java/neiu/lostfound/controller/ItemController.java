package neiu.lostfound.controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neiu.lostfound.dto.FoundItemRequest;
import neiu.lostfound.dto.LostItemRequest;
import neiu.lostfound.model.LostItem;
import neiu.lostfound.model.FoundItem;
import neiu.lostfound.service.ItemService;
import neiu.lostfound.service.MatchingService;
import neiu.lostfound.model.ItemMatch;
import neiu.lostfound.model.ReturnedItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
  private static final Logger log = LoggerFactory.getLogger(ItemController.class);
  private final ItemService items;
  private final MatchingService matchingService;

  public ItemController(ItemService items, MatchingService matchingService) {
    this.items = items;
    this.matchingService = matchingService;
  }

  @GetMapping("/lost/my")
  public ResponseEntity<List<LostItem>> myLostItems() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String userEmail = auth.getName(); // Spring Security uses email as username
    log.info("Fetching lost items for user: {}", userEmail);
    
    try {
      List<LostItem> userItems = items.listLostByUser(userEmail);
      return ResponseEntity.ok(userItems);
    } catch (Exception e) {
      log.error("Error fetching lost items for user {}: {}", userEmail, e.getMessage());
      return ResponseEntity.status(500).build();
    }
  }
  @PostMapping("/lost")
  public ResponseEntity<?> lost(@Valid @RequestBody LostItemRequest req) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String userEmail = auth.getName();
    log.info("Lost item reported: title={}, userEmail={}", req.title, userEmail);
    
    try {
      LostItem created = items.createLost(req, userEmail);
      return ResponseEntity.status(201).body(created);
    } catch (Exception e) {
      log.error("Error creating lost item: {}", e.getMessage());
      return ResponseEntity.status(500).build();
    }
  }

  @PostMapping("/found")
  public ResponseEntity<?> found(@Valid @RequestBody FoundItemRequest req) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String userEmail = auth.getName();
    log.info("Found item reported: title={}, userEmail={}", req.title, userEmail);
    
    try {
      FoundItem created = items.createFound(req, userEmail);
      return ResponseEntity.status(201).body(created);
    } catch (Exception e) {
      log.error("Error creating found item: {}", e.getMessage());
      return ResponseEntity.status(500).build();
    }
  }

  @GetMapping("/lost")
  public ResponseEntity<List<LostItem>> searchLost(@RequestParam(required = false, name = "q") String q,
                                                  @RequestParam(required = false, name = "category") String category,
                                                  @RequestParam(required = false, name = "subcategory") String subcategory) {
    log.info("Searching lost items by keyword: {}, category: {}, subcategory: {}", q, category, subcategory);
    List<LostItem> results = items.searchLostItems(q, category, subcategory);
    return ResponseEntity.ok(results);
  }

  @GetMapping("/found")
  public ResponseEntity<List<FoundItem>> searchFound(@RequestParam(required = false, name = "q") String q,
                                                    @RequestParam(required = false, name = "category") String category,
                                                    @RequestParam(required = false, name = "subcategory") String subcategory) {
    log.info("Searching found items by keyword: {}, category: {}, subcategory: {}", q, category, subcategory);
    List<FoundItem> results = items.searchFoundItems(q, category, subcategory);
    return ResponseEntity.ok(results);
  }

  @GetMapping("/found/{id}/matches")
  public ResponseEntity<List<LostItem>> matchLostForFound(@PathVariable("id") Long foundId) {
    log.info("Finding matches for found item id={}", foundId);
    List<LostItem> matches = matchingService.findMatchesForFound(foundId);
    return ResponseEntity.ok(matches);
  }

  @PostMapping("/lost/{lostId}/confirm-match/{foundId}")
  public ResponseEntity<?> confirmMatch(@PathVariable("lostId") Long lostId, @PathVariable("foundId") Long foundId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String adminUser = auth.getName(); // Get authenticated user email
    log.info("Admin confirming match: lostId={}, foundId={}, admin={}", lostId, foundId, adminUser);
    ItemMatch match = matchingService.confirmMatch(lostId, foundId, adminUser);
    if (match != null) {
      return ResponseEntity.ok().body(match);
    } else {
      return ResponseEntity.status(404).body("Lost or found item not found.");
    }
  }

  @GetMapping("/matches/all")
  public ResponseEntity<?> getAllMatches() {
    log.info("Fetching all found items with matches and confidence scores");
    try {
      var allMatches = matchingService.getAllFoundItemsWithMatches();
      return ResponseEntity.ok(allMatches);
    } catch (Exception e) {
      log.error("Error fetching matches: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body("Error fetching matches: " + e.getMessage());
    }
  }
}
