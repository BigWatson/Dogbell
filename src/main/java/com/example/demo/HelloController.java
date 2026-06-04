package com.example.demo;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

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
  public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
    String username = body.get("username");
    String password = body.get("password");
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "username and password required"));
    }
    boolean ok = userService.authenticate(username.trim(), password);
    if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid credentials"));
    boolean requires2fa = userService.isTwoFactorEnabled(username.trim());
    if (requires2fa) {
      session.setAttribute("pending2fa", username.trim());
      return ResponseEntity.ok(Map.of("twoFactor", true));
    }
    session.setAttribute("username", username.trim());
    return ResponseEntity.ok(Map.of("message", "login successful", "twoFactor", false));
  }

  @PostMapping("/api/logout")
  public ResponseEntity<?> logout(HttpSession session) {
    session.invalidate();
    return ResponseEntity.ok(Map.of("message", "logged out"));
  }

  @GetMapping("/api/me")
  public ResponseEntity<?> me(HttpSession session) {
    String username = (String) session.getAttribute("username");
    return ResponseEntity.ok(Map.of("username", username));
  }

  @PostMapping("/api/2fa/setup")
  public ResponseEntity<?> setup2fa(HttpSession session) {
    String username = (String) session.getAttribute("username");
    String secret = userService.generateTotpSecret();
    String issuer = "Team4U";
    String otpAuth = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, username, secret, issuer);
    return ResponseEntity.ok(Map.of("secret", secret, "otpAuthUrl", otpAuth));
  }

  @PostMapping("/api/2fa/enable")
  public ResponseEntity<?> enable2fa(@RequestBody Map<String, String> body, HttpSession session) {
    String username = (String) session.getAttribute("username");
    String secret = body.get("secret");
    String codeS = body.get("code");
    if (secret == null || secret.isBlank() || codeS == null || codeS.isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "secret and code required"));
    }
    int code;
    try { code = Integer.parseInt(codeS); } catch (Exception e) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","invalid code")); }
    boolean ok = userService.verifyTotp(secret, code);
    if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","invalid totp code"));
    boolean enabled = userService.enableTotpForUser(username, secret);
    if (!enabled) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","failed to enable"));
    return ResponseEntity.ok(Map.of("message","2fa enabled"));
  }

  @PostMapping("/api/2fa/verify")
  public ResponseEntity<?> verify2fa(@RequestBody Map<String, String> body, HttpSession session) {
    String pending = (String) session.getAttribute("pending2fa");
    if (pending == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "no pending 2FA login"));
    }
    String codeS = body.get("code");
    if (codeS == null || codeS.isBlank()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","code required"));
    int code;
    try { code = Integer.parseInt(codeS); } catch (Exception e) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","invalid code")); }
    String secret = userService.getTotpSecretForUser(pending);
    if (secret == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","2fa not enabled"));
    boolean ok = userService.verifyTotp(secret, code);
    if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","invalid code"));
    session.removeAttribute("pending2fa");
    session.setAttribute("username", pending);
    return ResponseEntity.ok(Map.of("message","2fa verified"));
  }
 
}