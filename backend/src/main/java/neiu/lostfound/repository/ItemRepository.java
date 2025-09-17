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
}
