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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

  @GetMapping("/found")
  public ResponseEntity<List<FoundItem>> allFound() {
    log.info("Fetching all found items");
    return ResponseEntity.ok(items.listFound());
  }

  @GetMapping("/lost")
  public ResponseEntity<List<LostItem>> allLost() {
    log.info("Fetching all lost items");
    return ResponseEntity.ok(items.listLost());
  }
  @PostMapping("/lost")
  public ResponseEntity<?> lost(@Valid @RequestBody LostItemRequest req,
                                   @RequestHeader(value="Authorization", required=false) String auth,
                                   @RequestHeader(value="X-UID", required=false) String uid) {
    log.info("Lost item reported: title={}, userId={}", req.title, uid);
    Long userId = null;
    try {
      userId = uid != null ? Long.valueOf(uid) : null;
    } catch (Exception e) { log.warn("Invalid userId format"); }
    LostItem created = items.createLost(req, userId);
    return ResponseEntity.status(201).body(created);
  }

  @PostMapping("/found")
  public ResponseEntity<?> found(@Valid @RequestBody FoundItemRequest req,
                                    @RequestHeader(value="Authorization", required=false) String auth,
                                    @RequestHeader(value="X-UID", required=false) String uid) {
    log.info("Found item reported: title={}, userId={}", req.title, uid);
    Long userId = null;
    try {
      userId = uid != null ? Long.valueOf(uid) : null;
    } catch (Exception e) { log.warn("Invalid userId format"); }
    FoundItem created = items.createFound(req, userId);
    return ResponseEntity.status(201).body(created);
  }

  @GetMapping("/lost/search")
  public ResponseEntity<List<LostItem>> searchLost(@RequestParam(required = false, name = "q") String q,
                                                  @RequestParam(required = false, name = "category") String category,
                                                  @RequestParam(required = false, name = "subcategory") String subcategory) {
    log.info("Searching lost items by keyword: {}, category: {}, subcategory: {}", q, category, subcategory);
    List<LostItem> results = items.searchLostItems(q, category, subcategory);
    return ResponseEntity.ok(results);
  }

  @GetMapping("/found/search")
  public ResponseEntity<List<FoundItem>> searchFound(@RequestParam(required = false, name = "q") String q,
                                                    @RequestParam(required = false, name = "category") String category,
                                                    @RequestParam(required = false, name = "subcategory") String subcategory) {
    log.info("Searching found items by keyword: {}, category: {}, subcategory: {}", q, category, subcategory);
    List<FoundItem> results = items.searchFoundItems(q, category, subcategory);
    return ResponseEntity.ok(results);
  }

  @GetMapping("/lost/{id}/matches")
  public ResponseEntity<List<FoundItem>> matchFoundForLost(@PathVariable("id") Long lostId) {
    log.info("Finding matches for lost item id={}", lostId);
    List<FoundItem> matches = matchingService.findMatchesForLost(lostId);
    return ResponseEntity.ok(matches);
  }

  @PostMapping("/lost/{lostId}/confirm-match/{foundId}")
  public ResponseEntity<?> confirmMatch(@PathVariable("lostId") Long lostId, @PathVariable("foundId") Long foundId, @RequestHeader(value="X-Admin", required=false) String adminUser) {
    log.info("Admin confirming match: lostId={}, foundId={}, admin={}", lostId, foundId, adminUser);
    ItemMatch match = matchingService.confirmMatch(lostId, foundId, adminUser);
    if (match != null) {
      return ResponseEntity.ok().body(match);
    } else {
      return ResponseEntity.status(404).body("Lost or found item not found.");
    }
  }

  @PostMapping("/lost/{lostId}/return/{foundId}")
  public ResponseEntity<?> returnItem(@PathVariable("lostId") Long lostId, @PathVariable("foundId") Long foundId) {
    log.info("Returning items: lostId={}, foundId={}", lostId, foundId);
    matchingService.returnItem(lostId, foundId);
    return ResponseEntity.ok().body("Items returned and moved to returned_items.");
  }
}
