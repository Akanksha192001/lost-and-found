package neiu.lostfound.repository;

import neiu.lostfound.model.Item;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ItemRepository {
  private final Map<String, Item> store = new ConcurrentHashMap<>();

  public Item save(Item it) { store.put(it.id, it); return it; }
  public List<Item> findAll() { return new ArrayList<>(store.values()); }
  public Optional<Item> findDuplicateFound(String title, String location, String dateISO) {
    return store.values().stream()
        .filter(it -> "FOUND".equals(it.type))
        .filter(it -> it.title != null && it.title.equalsIgnoreCase(title))
        .filter(it -> it.location != null && it.location.equalsIgnoreCase(location))
        .filter(it -> dateISO == null || Objects.equals(it.dateISO, dateISO))
        .findFirst();
  }
}
