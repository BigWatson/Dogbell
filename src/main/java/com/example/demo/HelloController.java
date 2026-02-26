package com.example.demo;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.UserService;

@RestController
public class HelloController {

  private final UserService userService;

  public HelloController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/")
  public String index() {
    return "Greetings from Spring Boot!";
  }

  @GetMapping("/hello/about")
  public String about() {
    return "About Us endpoint (stub) - describe your team here";
  }

  // API endpoints for the frontend pages
  @PostMapping("/api/signup")
  public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String password = body.get("password");
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "username and password required"));
    }
    boolean ok = userService.register(username.trim(), password);
    if (!ok) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "username already exists"));
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "user created"));
  }

  @PostMapping("/api/login")
  public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String password = body.get("password");
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "username and password required"));
    }
    boolean ok = userService.authenticate(username.trim(), password);
    if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid credentials"));
    boolean requires2fa = userService.isTwoFactorEnabled(username.trim());
    if (requires2fa) {
      return ResponseEntity.ok(Map.of("twoFactor", true));
    }
    return ResponseEntity.ok(Map.of("message", "login successful", "twoFactor", false));
  }

  @PostMapping("/api/2fa/setup")
  public ResponseEntity<?> setup2fa(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    if (username == null || username.isBlank()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "username required"));
    String secret = userService.generateTotpSecret();
    // otpauth URL for authenticator apps
    String issuer = "Team4U";
    String otpAuth = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, username.trim(), secret, issuer);
    return ResponseEntity.ok(Map.of("secret", secret, "otpAuthUrl", otpAuth));
  }

  @PostMapping("/api/2fa/enable")
  public ResponseEntity<?> enable2fa(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String secret = body.get("secret");
    String codeS = body.get("code");
    if (username == null || username.isBlank() || secret == null || secret.isBlank() || codeS == null || codeS.isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "username, secret and code required"));
    }
    int code;
    try { code = Integer.parseInt(codeS); } catch (Exception e) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","invalid code")); }
    boolean ok = userService.verifyTotp(secret, code);
    if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","invalid totp code"));
    boolean enabled = userService.enableTotpForUser(username.trim(), secret);
    if (!enabled) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","failed to enable"));
    return ResponseEntity.ok(Map.of("message","2fa enabled"));
  }

  @PostMapping("/api/2fa/verify")
  public ResponseEntity<?> verify2fa(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String codeS = body.get("code");
    if (username == null || username.isBlank() || codeS == null || codeS.isBlank()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","username and code required"));
    int code;
    try { code = Integer.parseInt(codeS); } catch (Exception e) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","invalid code")); }
    String secret = userService.getTotpSecretForUser(username.trim());
    if (secret == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","2fa not enabled"));
    boolean ok = userService.verifyTotp(secret, code);
    if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","invalid code"));
    return ResponseEntity.ok(Map.of("message","2fa verified"));
  }
 
}