package neiu.lostfound.controller;

import neiu.lostfound.dto.LoginRequest;
import neiu.lostfound.dto.RegisterRequest;
import neiu.lostfound.model.User;
import neiu.lostfound.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/register")
  public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest req) {
    return ResponseEntity.ok(auth.register(req));
  }

  @PostMapping("/login")
  public ResponseEntity<User> login(@Valid @RequestBody LoginRequest req) {
    return ResponseEntity.ok(auth.login(req));
  }
}
