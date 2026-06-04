package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.model.PiDevice;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

/**
 * Stores Pi device records (piId -> PiDevice) in a plain JSON file.
 *
 * Pre-seeded Pi IDs are loaded from the 'pi.seeded-ids' property (comma-separated).
 * Each ID is inserted as unregistered on first startup if it does not already exist.
 *
 * NOTE: File-based storage is suitable for single-instance development / demo use.
 * It does not support concurrent server instances and offers no transactional guarantees.
 * For production, migrate to a database (e.g. H2, PostgreSQL, or SQLite).
 */
@Service
public class PiStore {
    private static final Logger log = LoggerFactory.getLogger(PiStore.class);

    private final Path storePath;
    private final List<String> seededIds;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, PiDevice> devices = new ConcurrentHashMap<>();

    public PiStore(
            @Value("${pistore.path:pistore.json}") String path,
            @Value("${pi.seeded-ids:DOGBELL-001,DOGBELL-002,DOGBELL-003}") String seededIdsCsv) throws IOException {
        this.storePath = Path.of(path);
        this.seededIds = Arrays.asList(seededIdsCsv.split(","));
    }

    @PostConstruct
    public void load() {
        if (Files.exists(storePath)) {
            try {
                byte[] json = Files.readAllBytes(storePath);
                Map<String, PiDevice> loaded = mapper.readValue(json, new TypeReference<>() {});
                devices.putAll(loaded);
                // Migrate any legacy single-phone records to contacts list
                devices.values().forEach(PiDevice::migrateLegacy);
                log.info("Loaded {} Pi device(s) from {}", devices.size(), storePath);
            } catch (Exception e) {
                log.warn("Failed to load Pi store — starting fresh", e);
            }
        }

        // Seed any IDs that aren't already in the store
        for (String id : seededIds) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty() && !devices.containsKey(trimmed)) {
                devices.put(trimmed, new PiDevice(trimmed));
                log.info("Seeded unregistered Pi: {}", trimmed);
            }
        }
        save();
    }

    public synchronized void save() {
        try {
            byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(devices);
            Files.write(storePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save Pi store", e);
        }
    }

    /** Returns null if the piId is not in the store (unrecognized device). */
    public PiDevice get(String piId) {
        return devices.get(piId);
    }

    public void put(PiDevice device) {
        devices.put(device.getPiId(), device);
        save();
    }

    /**
     * Atomically check that a Pi exists and is unregistered, then register it.
     * Returns "OK" on success, or an error key ("NOT_FOUND", "ALREADY_REGISTERED").
     */
    public synchronized String tryRegister(String piId, String username,
                                           java.util.List<PiDevice.PhoneContact> contacts) {
        PiDevice device = devices.get(piId);
        if (device == null) return "NOT_FOUND";
        if (device.isRegistered()) return "ALREADY_REGISTERED";
        device.setUsername(username);
        device.setContacts(contacts);
        device.setRegistered(true);
        save();
        return "OK";
    }

    public boolean exists(String piId) {
        return devices.containsKey(piId);
    }

    /** Returns all devices in the store. */
    public Map<String, PiDevice> getAll() {
        return new java.util.HashMap<>(devices);
    }
}
