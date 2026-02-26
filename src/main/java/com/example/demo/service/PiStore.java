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

    public boolean exists(String piId) {
        return devices.containsKey(piId);
    }
}
