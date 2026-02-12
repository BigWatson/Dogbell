package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal phone endpoints to illustrate flow. Integrate a provider like Twilio
 * or AWS SNS to send/verify OTPs. This controller currently contains placeholders
 * and explanatory comments where provider integration is required.
 */
@RestController
@RequestMapping("/api/phone")
public class PhoneController {

    @PostMapping("/request")
    public ResponseEntity<?> requestOtp(@RequestBody Map<String,String> body) {
        String phone = body.get("phoneNumber");
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "phoneNumber required"));
        }

        // TODO: generate OTP, store hash in Redis with TTL, call Twilio/AWS SNS to send SMS

        return ResponseEntity.ok(Map.of("status","OTP_SENT"));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String,String> body) {
        String phone = body.get("phoneNumber");
        String code = body.get("code");
        if (phone == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error","phoneNumber and code required"));
        }

        // TODO: verify OTP from Redis/DB, attach phone to authenticated user account

        return ResponseEntity.ok(Map.of("status","VERIFIED"));
    }
}
