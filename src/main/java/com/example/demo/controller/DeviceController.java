package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.mqtt.MqttPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Controller that exposes device-related endpoints used by the UI.
 *
 * The important endpoint implemented below is `/api/device/register-request` which sends
 * a `REGISTER_REQUEST` MQTT message to the target device topic. The device should be
 * subscribed to `devices/{deviceId}/commands` and respond with an ACK on
 * `devices/{deviceId}/events`.
 */
@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final MqttPublisher publisher;
    private final ObjectMapper mapper;

    @Autowired
    public DeviceController(MqttPublisher publisher, ObjectMapper mapper) {
        this.publisher = publisher;
        this.mapper = mapper;
    }

    /**
     * Request device registration. The backend builds a registration request containing
     * a generated requestId and (optionally) a phone reference. It publishes the request
     * to the device topic and returns the requestId to the UI so it can wait for confirmation.
     */
    @PostMapping("/register-request")
    public ResponseEntity<?> registerRequest(@RequestBody RegisterRequest req) throws Exception {
        if (req.getDeviceId() == null || req.getDeviceId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "deviceId is required"));
        }

        String requestId = UUID.randomUUID().toString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "REGISTER_REQUEST");
        payload.put("requestId", requestId);
        // phoneRef should be a short token or DB id; avoid passing raw PII when possible.
        payload.put("phoneRef", req.getPhoneRef());

        String json = mapper.writeValueAsString(payload);

        try {
            publisher.publishCommand(req.getDeviceId(), json);
        } catch (MqttException e) {
            return ResponseEntity.status(502).body(Map.of("error", "failed to publish to device", "detail", e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("requestId", requestId, "status", "SENT"));
    }
}
