package com.example.demo.mqtt;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.model.PiDevice;
import com.example.demo.service.PiStore;
import com.example.demo.sms.SmsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Component
public class MqttEventListener implements MqttCallback {

    private final MqttClient client;
    private final ObjectMapper mapper;
    private final SmsService smsService;
    private final PiStore piStore;

    @Autowired
    public MqttEventListener(MqttClient client, ObjectMapper mapper, SmsService smsService, PiStore piStore) {
        this.client = client;
        this.mapper = mapper;
        this.smsService = smsService;
        this.piStore = piStore;
    }

    @PostConstruct
    public void init() throws MqttException {
        client.setCallback(this);

        new Thread(() -> {
            int attempts = 0;
            while (attempts < 30) {
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
        System.err.println("MQTT connection lost: " + cause.getMessage());
        new Thread(() -> {
            int attempts = 0;
            while (attempts < 30) {
                try {
                    if (client.isConnected()) {
                        client.subscribe("devices/+/events", 1);
                        System.out.println("Resubscribed to devices/+/events after reconnect");
                        return;
                    }
                } catch (MqttException e) {
                    System.err.println("Resubscribe failed: " + e.getMessage());
                }
                attempts++;
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            System.err.println("Could not resubscribe after reconnect");
        }, "mqtt-resubscriber").start();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        JsonNode node = mapper.readTree(payload);
        String type = node.path("type").asText(null);

        if ("DOORBELL_RING".equals(type)) {
            String deviceId = extractDeviceIdFromTopic(topic);
            PiDevice device = piStore.get(deviceId);
            if (device == null || !device.isRegistered()) {
                System.err.println("DOORBELL_RING from unregistered Pi: " + deviceId);
                return;
            }
            // Send SMS to all registered contacts for this device
            for (PiDevice.PhoneContact contact : device.getContacts()) {
                boolean ok = smsService.sendSms(contact.getPhone(), "Your dog is at the door!", contact.getGatewayEmail());
                System.out.printf("Doorbell SMS to %s/%s (Pi %s) result=%s\n",
                        device.getUsername(), contact.getPhone(), deviceId, ok);
            }
            return;
        }

        if ("SEND_SMS_REQUEST".equals(type)) {
            String to = node.path("to").asText(null);
            String body = node.path("body").asText("");
            String requestId = node.path("requestId").asText(null);
            String smtpGateway = node.path("smtpGateway").asText(null);

            if (to == null || to.isBlank()) {
                System.err.println("SEND_SMS_REQUEST missing 'to' field");
                return;
            }

            boolean ok = smsService.sendSms(to, body, smtpGateway);
            System.out.printf("SMS request %s to %s result=%s\n", requestId, to, ok);

            try {
                String deviceId = extractDeviceIdFromTopic(topic);
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
    public void deliveryComplete(IMqttDeliveryToken token) {}

    private String extractDeviceIdFromTopic(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 3) return parts[1];
        return "";
    }
}
