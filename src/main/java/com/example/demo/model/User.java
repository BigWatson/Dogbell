package com.example.demo.model;

import java.time.Instant;

public class User {
  private String username;
  private String passwordHash; // base64
  private String salt; // base64
  private boolean twoFactorEnabled = false;
  private String totpSecret; // base32 secret for TOTP clients
  private String createdAt;

  public User() {}

  public User(String username, String passwordHash, String salt) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.salt = salt;
    this.createdAt = Instant.now().toString();
  }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public String getSalt() { return salt; }
  public void setSalt(String salt) { this.salt = salt; }

  public String getTotpSecret() { return totpSecret; }
  public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

  public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
  public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

  public String getCreatedAt() { return createdAt; }
  public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
