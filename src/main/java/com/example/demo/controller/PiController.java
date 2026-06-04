package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.PiDevice;
import com.example.demo.model.PiDevice.PhoneContact;
import com.example.demo.mqtt.MqttPublisher;
import com.example.demo.service.PiStore;
import com.example.demo.service.UserStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles Pi device registration.
 *
 * POST /api/pi/register
 *   Body: { "piId": "DOGBELL-001", "username": "alice",
 *           "contacts": [
 *             { "phone": "17607042102", "gatewayEmail": "17607042102@tmomail.net" },
 *             { "phone": "15551234567", "gatewayEmail": "15551234567@vtext.com" }
 *           ] }
 *
 * Also accepts legacy single-phone format:
 *   Body: { "piId": "...", "username": "...", "phone": "...", "gatewayEmail": "..." }
 */
@RestController
@RequestMapping("/api/pi")
public class PiController {

    private final PiStore piStore;
    private final UserStore userStore;
    private final MqttPublisher mqttPublisher;
    private final ObjectMapper mapper;

    public PiController(PiStore piStore, UserStore userStore, MqttPublisher mqttPublisher, ObjectMapper mapper) {
        this.piStore = piStore;
        this.userStore = userStore;
        this.mqttPublisher = mqttPublisher;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody JsonNode body) {
        String piId     = body.path("piId").asText(null);
        String username = body.path("username").asText(null);

        if (piId == null || piId.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "piId required"));
        if (username == null || username.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));

        // Build contacts list from either "contacts" array or legacy single phone/gateway
        List<PhoneContact> contacts = new ArrayList<>();
        JsonNode contactsNode = body.get("contacts");
        if (contactsNode != null && contactsNode.isArray()) {
            for (JsonNode c : contactsNode) {
                String phone = c.path("phone").asText("").trim();
                String gw    = c.path("gatewayEmail").asText("").trim();
                if (!phone.isEmpty()) {
                    contacts.add(new PhoneContact(phone, gw));
                }
            }
        } else {
            // Legacy single-phone format
            String phone   = body.path("phone").asText(null);
            String gateway = body.path("gatewayEmail").asText("");
            if (phone != null && !phone.isBlank()) {
                contacts.add(new PhoneContact(phone.trim(), gateway.trim()));
            }
        }

        if (contacts.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "At least one phone number is required"));

        // User must have an account
        if (!userStore.exists(username))
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));

        // Atomically check and register the Pi (prevents race conditions)
        String result = piStore.tryRegister(piId, username, contacts);
        if ("NOT_FOUND".equals(result))
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pi ID not recognised"));
        if ("ALREADY_REGISTERED".equals(result))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Pi already registered"));

        // Send registration credentials to the Pi via MQTT
        try {
            Map<String, Object> registrationMsg = new HashMap<>();
            registrationMsg.put("type", "REGISTER_PI");
            registrationMsg.put("username", username);
            registrationMsg.put("contacts", contacts);
            String json = mapper.writeValueAsString(registrationMsg);
            mqttPublisher.publishCommand(piId, json);
        } catch (Exception e) {
            System.err.println("Warning: Failed to publish registration to Pi " + piId + ": " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "status", "REGISTERED",
                "piId", piId,
                "username", username,
                "contactCount", contacts.size()
        ));
    }
}
