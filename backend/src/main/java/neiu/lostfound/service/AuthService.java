package neiu.lostfound.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import neiu.lostfound.dto.LoginRequest;
import neiu.lostfound.dto.RegisterRequest;
import neiu.lostfound.model.User;
import neiu.lostfound.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private final UserRepository repo;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UserRepository repo, PasswordEncoder passwordEncoder) {
    this.repo = repo;
    this.passwordEncoder = passwordEncoder;
  }

  public User findByEmail(String email) {
    log.debug("Finding user by email: {}", email);
    return repo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
  }

  public User register(RegisterRequest req) {
    log.info("Registering user: {}", req.email);
    User u = new User();
    u.setName(req.name);
    u.setEmail(req.email);
    u.setRole("USER");
    // Demo only: not secure; replace with BCrypt later
    u.setPassword(passwordEncoder.encode(req.password));
    return repo.save(u);
  }

  public User login(LoginRequest req) {
    log.info("Authenticating user: {}", req.email);
    return repo.findByEmail(req.email)
      .filter(u -> u.getPassword() != null && passwordEncoder.matches(req.password, u.getPassword()))
      .orElseThrow(() -> new RuntimeException("Invalid credentials"));
  }
}
