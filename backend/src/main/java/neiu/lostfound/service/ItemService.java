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
    it.title = req.title;
    it.description = req.description;
    it.location = req.locationLastSeen;
    it.dateISO = req.dateLost;
    it.createdByUserId = userId;
    return repo.save(it);
  }

  public Item createFound(FoundItemRequest req, String userId) {
    Item it = new Item();
    it.id = UUID.randomUUID().toString();
    it.type = "FOUND";
    it.title = req.title;
    it.description = req.description;
    it.location = req.locationFound;
    it.dateISO = req.dateFound;
    it.createdByUserId = userId;
    return repo.save(it);
  }

  public List<Item> list() { return repo.findAll(); }
}
