package com.example.demo.mqtt;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.sms.SmsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

/**
 * Subscribes to device events (devices/+/events) and handles SEND_SMS_REQUEST messages.
 * When a SEND_SMS_REQUEST arrives, it validates the payload and calls SmsService to send the SMS.
 */
@Component
public class MqttEventListener implements MqttCallback {

    private final MqttClient client;
    private final ObjectMapper mapper;
    private final SmsService smsService;

    @Autowired
    public MqttEventListener(MqttClient client, ObjectMapper mapper, SmsService smsService) {
        this.client = client;
        this.mapper = mapper;
        this.smsService = smsService;
    }

    @PostConstruct
    public void init() throws MqttException {
        client.setCallback(this);

        // Defer subscription until the client is connected. If the client is not
        // yet connected (MQTT connect is performed asynchronously), start a
        // small background thread that waits and subscribes when ready.
        new Thread(() -> {
            int attempts = 0;
            while (attempts < 10) {
                try {
                    if (client.isConnected()) {
                        client.subscribe("devices/+/events", 1);
                        System.out.println("Subscribed to devices/+/events");
                        return;
                    }
                } catch (MqttException e) {
                    System.err.println("Failed to subscribe: " + e.getMessage());
                }
                attempts++;
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            System.err.println("Could not subscribe to devices/+/events after retries");
        }, "mqtt-subscriber").start();
    }

    @Override
    public void connectionLost(Throwable cause) {
        // Connection lost handling: logging and reconnection will be handled by the Paho options
        System.err.println("MQTT connection lost: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        JsonNode node = mapper.readTree(payload);
        String type = node.path("type").asText(null);

        if ("SEND_SMS_REQUEST".equals(type)) {
            String to = node.path("to").asText(null);
            String body = node.path("body").asText("");
            String requestId = node.path("requestId").asText(null);
            String smtpGateway = node.path("smtpGateway").asText(null); // optional

            if (to == null || to.isBlank()) {
                System.err.println("SEND_SMS_REQUEST missing 'to' field");
                return;
            }

            boolean ok = smsService.sendSms(to, body, smtpGateway);
            System.out.printf("SMS request %s to %s result=%s\n", requestId, to, ok);

            // Optionally, publish a result back to the device commands topic
            try {
                String deviceId = extractDeviceIdFromTopic(topic); // devices/{deviceId}/events
                String resultTopic = String.format("devices/%s/commands", deviceId);
                String resultJson = mapper.writeValueAsString(new java.util.HashMap<String,Object>(){{
                    put("type","SEND_SMS_RESULT");
                    put("requestId", requestId);
                    put("success", ok);
                }});
                client.publish(resultTopic, new MqttMessage(resultJson.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                System.err.println("Failed to publish SEND_SMS_RESULT: " + e.getMessage());
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not used for subscriptions
    }

    private String extractDeviceIdFromTopic(String topic) {
        // topic is devices/{deviceId}/events
        String[] parts = topic.split("/");
        if (parts.length >= 3) return parts[1];
        return "";
    }
}
