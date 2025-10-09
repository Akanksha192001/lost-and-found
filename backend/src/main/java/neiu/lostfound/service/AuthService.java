package neiu.lostfound.service;

import neiu.lostfound.dto.LoginRequest;
import neiu.lostfound.dto.RegisterRequest;
import neiu.lostfound.model.User;
import neiu.lostfound.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
public class AuthService {
  private final UserRepository repo;

  public AuthService(UserRepository repo) {
    this.repo = repo;
  }

  public User register(RegisterRequest req) {
    User u = new User();
    u.id = UUID.randomUUID().toString();
    u.name = req.name;
    u.email = req.email;
    String role = req.role == null ? "STUDENT" : req.role.trim().toUpperCase();
    if (!Arrays.asList("STUDENT", "ADMIN", "SECURITY").contains(role)) {
      role = "STUDENT";
    }
    u.role = role;
    // Demo only: not secure; replace with BCrypt later
    u.passwordHash = "{noop}" + req.password;
    return repo.save(u);
  }

  public User login(LoginRequest req) {
    return repo.findByEmail(req.email)
      .filter(u -> u.passwordHash != null && u.passwordHash.equals("{noop}" + req.password))
      .orElseThrow(() -> new RuntimeException("Invalid credentials"));
  }
}
