package com.example.demo.service;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Service;

import com.example.demo.model.User;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

@Service
public class UserService {
  private final UserStore store;
  private static final int ITERATIONS = 65536;
  private static final int KEY_LENGTH = 256;
  private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

  public UserService(UserStore store) {
    this.store = store;
  }

  public synchronized boolean register(String username, String password) {
    if (store.exists(username)) return false;
    try {
      byte[] salt = new byte[16];
      new SecureRandom().nextBytes(salt);
      String saltB64 = Base64.getEncoder().encodeToString(salt);
      String hash = hashPassword(password, salt);
      User u = new User(username, hash, saltB64);
      store.put(u);
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized boolean authenticate(String username, String password) {
    User u = store.get(username);
    if (u == null) return false;
    try {
      String expected = u.getPasswordHash();
      byte[] salt = Base64.getDecoder().decode(u.getSalt());
      String got = hashPassword(password, salt);
      return constantTimeEquals(expected, got);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized String generateTotpSecret() {
    GoogleAuthenticatorKey key = gAuth.createCredentials();
    return key.getKey(); // base32 secret
  }

  public synchronized boolean verifyTotp(String secret, int code) {
    try {
      return gAuth.authorize(secret, code);
    } catch (Exception e) {
      return false;
    }
  }

  public synchronized boolean enableTotpForUser(String username, String secret) {
    User u = store.get(username);
    if (u == null) return false;
    u.setTotpSecret(secret);
    u.setTwoFactorEnabled(true);
    store.put(u);
    return true;
  }

  public synchronized boolean isTwoFactorEnabled(String username) {
    User u = store.get(username);
    return u != null && u.isTwoFactorEnabled();
  }

  public synchronized String getTotpSecretForUser(String username) {
    User u = store.get(username);
    return u == null ? null : u.getTotpSecret();
  }

  private String hashPassword(String password, byte[] salt) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    byte[] hash = skf.generateSecret(spec).getEncoded();
    return Base64.getEncoder().encodeToString(hash);
  }

  private boolean constantTimeEquals(String a, String b) {
    byte[] aa = a.getBytes();
    byte[] bb = b.getBytes();
    if (aa.length != bb.length) return false;
    int result = 0;
    for (int i = 0; i < aa.length; i++) result |= aa[i] ^ bb[i];
    return result == 0;
  }
}
