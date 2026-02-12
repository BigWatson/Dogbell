package com.example.demo.mqtt;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple MQTT publisher service.
 *
 * Responsible for constructing device topics and publishing JSON payloads.
 * The service ensures the client is connected and publishes with QoS=1.
 */
@Service
public class MqttPublisher {

    private final MqttClient client;
    private final ObjectMapper mapper;

    @Autowired
    public MqttPublisher(MqttClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * Publish a JSON payload string to the device commands topic.
     * @param deviceId device identifier (used to form topic)
     * @param payload JSON string payload
     */
    public void publishCommand(String deviceId, String payload) throws MqttException {
        String topic = String.format("devices/%s/commands", deviceId);
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1); // delivery at least once

        if (!client.isConnected()) {
            // attempt reconnect - in production use exponential backoff
            client.reconnect();
        }

        client.publish(topic, message);
    }
}
