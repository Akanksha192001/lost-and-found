package neiu.lostfound.controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;


import neiu.lostfound.dto.LoginRequest;
import neiu.lostfound.dto.RegisterRequest;
import neiu.lostfound.model.User;
import neiu.lostfound.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private final AuthService auth;
  private final AuthenticationManager authenticationManager;

  public AuthController(AuthService auth, AuthenticationManager authenticationManager) {
    this.auth = auth;
    this.authenticationManager = authenticationManager;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
    log.info("Register request for email: {}", req.email);
    User user = auth.register(req);
    return ResponseEntity.ok(java.util.Collections.singletonMap("name", user.getName()));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
    log.info("Login attempt for email: {}", req.email);
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.email, req.password)
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
    User user = auth.findByEmail(req.email);
    return ResponseEntity.ok(new Object() {
      public final String name = user.getName();
      public final String email = user.getEmail();
      public final String role = user.getRole();
    });
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    log.info("Logout request received");
    if (request.getSession(false) != null) {
      request.getSession(false).invalidate();
    }
    Cookie cookie = new Cookie("JSESSIONID", null);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(0);
    response.addCookie(cookie);
    return ResponseEntity.ok().build();
  }
}
