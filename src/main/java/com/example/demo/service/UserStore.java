package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

/**
 * NOTE: File-based encrypted storage is suitable for single-instance development / demo use.
 * It does not support concurrent server instances. For production, migrate to a database.
 */
@Service
public class UserStore {
  private static final Logger log = LoggerFactory.getLogger(UserStore.class);
  private final Path storePath;
  private final byte[] encryptionKey;
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, User> users = new ConcurrentHashMap<>();

  public UserStore(@Value("${userstore.path:userstore.dat}") String path,
                   @Value("${encryption.key:}") String base64Key) throws IOException {
    this.storePath = Path.of(path);
    if (base64Key == null || base64Key.isBlank()) {
      // generate random key (not ideal for production — set encryption.key in application.properties)
      byte[] k = new byte[32];
      new SecureRandom().nextBytes(k);
      this.encryptionKey = k;
    } else {
      this.encryptionKey = Base64.getDecoder().decode(base64Key);
    }
  }

  @PostConstruct
  public void load() {
    if (!Files.exists(storePath)) return;
    try {
      String base64 = Files.readString(storePath);
      byte[] json = CryptoUtils.decrypt(encryptionKey, base64);
      Map<String, User> loaded = mapper.readValue(json, new TypeReference<>() {});
      users.clear();
      users.putAll(loaded);
    } catch (Exception e) {
      log.warn("Failed to load user store — starting with empty store", e);
      return;
    }
  }

  public synchronized void save() {
    try {
      byte[] json = mapper.writeValueAsBytes(users);
      String base64 = CryptoUtils.encrypt(encryptionKey, json);
      Files.writeString(storePath, base64, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Exception e) {
      throw new RuntimeException("Failed to save user store", e);
    }
  }

  public Map<String, User> getUsers() {
    return Collections.unmodifiableMap(users);
  }

  public User get(String username) {
    return users.get(username.toLowerCase());
  }

  public void put(User u) {
    users.put(u.getUsername().toLowerCase(), u);
    save();
  }

  public boolean exists(String username) {
    return users.containsKey(username.toLowerCase());
  }
}
