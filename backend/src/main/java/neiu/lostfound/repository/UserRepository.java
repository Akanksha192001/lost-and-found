package neiu.lostfound.repository;

import neiu.lostfound.model.User;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {
  private final Map<String, User> store = new ConcurrentHashMap<>();

  public User save(User u) { store.put(u.id, u); return u; }
  public Optional<User> findByEmail(String email) {
    return store.values().stream()
        .filter(x -> x.email != null && x.email.equalsIgnoreCase(email))
        .findFirst();
  }
  public Optional<User> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }
}
