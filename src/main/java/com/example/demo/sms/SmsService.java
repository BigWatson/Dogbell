package com.example.demo.sms;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * SMS delivery service with two backends:
 *  - SMTP email->SMS gateway (via carrier email gateway)
 *  - Textbelt HTTP API (self-hosted or public)
 *
 * The Pi or backend should request SMS sending via MQTT events. This service centralizes
 * sending so credentials remain on the server.
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;
    private final HttpClient httpClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${sms.from:no-reply@example.com}")
    private String fromAddress;

    @Value("${textbelt.url:http://localhost:8080}")
    private String textbeltUrl;

    @Value("${textbelt.key:textbelt}")
    private String textbeltKey;

    public SmsService(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * Send SMS using Textbelt (HTTP) API.
     * @return true if the provider accepted the message (not a delivery guarantee)
     */
    public boolean sendViaTextbelt(String to, String body, String gatewayEmail) {
        try {
            java.util.Map<String, String> payload = java.util.Map.of(
                    "phone", to, "message", body, "key", textbeltKey);
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(textbeltUrl + "/text"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Textbelt response: status={} body={}", resp.statusCode(), resp.body());
            if (resp.statusCode() != 200) return false;
            com.fasterxml.jackson.databind.JsonNode respNode = objectMapper.readTree(resp.body());
            return respNode.path("success").asBoolean(false);
        } catch (Exception e) {
            log.warn("Textbelt failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send SMS via email->SMS gateway; recipient should be something like 1234567890@txt.att.net.
     */
    public boolean sendViaSmtp(String gatewayEmailAddress, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(gatewayEmailAddress);
            msg.setSubject("Dogbell Alert");
            msg.setText(body);
            mailSender.send(msg);
            log.info("SMTP sent to {}", gatewayEmailAddress);
            return true;
        } catch (Exception e) {
            log.error("SMTP failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Convenience wrapper: use SMTP email-to-SMS gateway when a carrier gateway address
     * is provided (most reliable for this project), otherwise fall back to Textbelt HTTP API.
     */
    public boolean sendSms(String to, String body, String smtpGatewayAddress) {
        if (smtpGatewayAddress != null && !smtpGatewayAddress.isBlank()) {
            return sendViaSmtp(smtpGatewayAddress, body);
        }
        return sendViaTextbelt(to, body, null);
    }
}
