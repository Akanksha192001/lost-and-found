package neiu.lostfound.service;

import neiu.lostfound.dto.FoundItemRequest;
import neiu.lostfound.dto.LostItemRequest;
import neiu.lostfound.model.Item;
import neiu.lostfound.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ItemService {
  private final ItemRepository repo;

  public ItemService(ItemRepository repo) {
    this.repo = repo;
  }

  public Item createLost(LostItemRequest req, String userId) {
    Item it = new Item();
    it.id = UUID.randomUUID().toString();
    it.type = "LOST";
    it.title = req.title == null ? null : req.title.trim();
    it.description = req.description;
    it.location = req.locationLastSeen == null ? null : req.locationLastSeen.trim();
    it.dateISO = req.dateLost == null ? null : req.dateLost.trim();
    it.createdByUserId = userId;
    it.status = "REPORTED";
    return repo.save(it);
  }

  public Item createFound(FoundItemRequest req, String userId) {
    String title = req.title == null ? "" : req.title.trim();
    String location = req.locationFound == null ? "" : req.locationFound.trim();
    String date = req.dateFound == null ? null : req.dateFound.trim();
    repo.findDuplicateFound(title, location, date)
        .ifPresent(existing -> { throw new RuntimeException("Similar found item already listed."); });
    Item it = new Item();
    it.id = UUID.randomUUID().toString();
    it.type = "FOUND";
    it.title = title;
    it.description = req.description;
    it.category = req.category == null ? null : req.category.trim();
    it.location = location;
    it.dateISO = date;
    it.createdByUserId = userId;
    it.photoData = req.photoData;
    it.status = "LISTED";
    return repo.save(it);
  }

  public List<Item> list() { return repo.findAll(); }
}
