package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.PiDevice;
import com.example.demo.service.PiStore;
import com.example.demo.service.UserStore;

/**
 * Handles Pi device registration.
 *
 * POST /api/pi/register
 *   Body: { "piId": "DOGBELL-001", "username": "alice",
 *           "phone": "17607042102", "gatewayEmail": "17607042102@tmomail.net" }
 *
 * Rules:
 *   - piId must be in the store (pre-seeded at the factory)
 *   - piId must not already be registered
 *   - username must exist
 */
@RestController
@RequestMapping("/api/pi")
public class PiController {

    private final PiStore piStore;
    private final UserStore userStore;

    public PiController(PiStore piStore, UserStore userStore) {
        this.piStore = piStore;
        this.userStore = userStore;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String piId       = body.get("piId");
        String username   = body.get("username");
        String phone      = body.get("phone");
        String gateway    = body.get("gatewayEmail");

        if (piId == null || piId.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "piId required"));
        if (username == null || username.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        if (phone == null || phone.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "phone required"));

        // Pi must be in the pre-seeded pool
        PiDevice device = piStore.get(piId);
        if (device == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pi ID not recognised"));

        // Pi must not already belong to someone
        if (device.isRegistered())
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Pi already registered"));

        // User must have an account
        if (!userStore.exists(username))
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));

        device.setUsername(username);
        device.setPhone(phone);
        device.setGatewayEmail(gateway != null ? gateway : "");
        device.setRegistered(true);
        piStore.put(device);

        return ResponseEntity.ok(Map.of(
                "status", "REGISTERED",
                "piId", piId,
                "username", username
        ));
    }
}
