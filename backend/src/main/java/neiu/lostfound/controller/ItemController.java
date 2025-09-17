package neiu.lostfound.controller;

import neiu.lostfound.dto.FoundItemRequest;
import neiu.lostfound.dto.LostItemRequest;
import neiu.lostfound.model.Item;
import neiu.lostfound.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
  private final ItemService items;

  public ItemController(ItemService items) {
    this.items = items;
  }

  @GetMapping
  public ResponseEntity<List<Item>> all() { return ResponseEntity.ok(items.list()); }

  @PostMapping("/lost")
  public ResponseEntity<Item> lost(@Valid @RequestBody LostItemRequest req,
                                   @RequestHeader(value="X-UID", required=false) String uid) {
    return ResponseEntity.ok(items.createLost(req, uid == null ? "demoUser" : uid));
  }

  @PostMapping("/found")
  public ResponseEntity<Item> found(@Valid @RequestBody FoundItemRequest req,
                                    @RequestHeader(value="X-UID", required=false) String uid) {
    return ResponseEntity.ok(items.createFound(req, uid == null ? "demoUser" : uid));
  }
}
