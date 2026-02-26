package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.sms.SmsService;

/**
 * Doorbell SMS trigger endpoint.
 * POST /api/doorbell/ring with {"phone":"+17607042102","message":"Doorbell is rung"}
 * Sends SMS via Textbelt or SMTP gateway.
 */
@RestController
@RequestMapping("/api/doorbell")
public class DoorBellController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/ring")
    public ResponseEntity<?> triggerDoorBellSms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String message = body.getOrDefault("message", "Doorbell is rung");
        String gatewayEmail = body.get("gatewayEmail");

        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone required"));
        }

        // Try Textbelt first, fall back to SMTP gateway if provided
        boolean success = smsService.sendSms(phone, message, gatewayEmail);

        if (success) {
            return ResponseEntity.ok(Map.of("status", "SMS_SENT", "phone", phone, "message", message));
        } else {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to send SMS",
                "hint", "Ensure Textbelt is running OR configure SMTP + gatewayEmail"
            ));
        }
    }
}
