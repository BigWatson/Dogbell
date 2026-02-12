package com.example.demo.sms;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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

    @Autowired(required = false)
    private JavaMailSender mailSender;
    private final HttpClient httpClient;

    @Value("${sms.from:no-reply@example.com}")
    private String fromAddress;

    @Value("${textbelt.url:http://localhost:8080}")
    private String textbeltUrl;

    @Value("${textbelt.key:textbelt}")
    private String textbeltKey;

    public SmsService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * Send SMS using Textbelt (HTTP) API.
     * @return true if the provider accepted the message (not a delivery guarantee)
     */
    public boolean sendViaTextbelt(String to, String body) {
        try {
            String json = String.format("{\"phone\":\"%s\",\"message\":\"%s\",\"key\":\"%s\"}",
                    escapeJson(to), escapeJson(body), escapeJson(textbeltKey));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(textbeltUrl + "/text"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            // Textbelt returns JSON; in a full implementation parse and check `success` field.
            return resp.statusCode() == 200;
        } catch (Exception e) {
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
            msg.setSubject("");
            msg.setText(body);
            mailSender.send(msg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convenience wrapper: try Textbelt first, fall back to SMTP gateway address if provided.
     */
    public boolean sendSms(String to, String body, String smtpGatewayAddressFallback) {
        boolean ok = sendViaTextbelt(to, body);
        if (ok) return true;
        if (smtpGatewayAddressFallback != null && !smtpGatewayAddressFallback.isBlank()) {
            return sendViaSmtp(smtpGatewayAddressFallback, body);
        }
        return false;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
