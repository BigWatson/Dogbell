package com.example.demo.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT configuration for the backend. Creates and connects an Eclipse Paho MQTT client.
 *
 * Configure the broker URL and optional credentials in `application.properties`:
 *
 * mqtt.broker=tcp://your-broker.example.com:8883
 * mqtt.username=yourUsername
 * mqtt.password=yourPassword
 * mqtt.client-id=optional-backend-client-id
 */
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker:tcp://broker.hivemq.com:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id:backend-${random.uuid}}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Bean
    public MqttClient mqttClient() throws Exception {
        // MemoryPersistence is fine for a backend publisher that reconnects automatically.
        MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        // avoid long blocking connect at startup
        opts.setConnectionTimeout(10); // seconds

        if (username != null && !username.isEmpty()) {
            opts.setUserName(username);
        }
        if (password != null && !password.isEmpty()) {
            opts.setPassword(password.toCharArray());
        }

        // Connect asynchronously to avoid blocking Spring startup. If connect fails
        // we log and rely on automaticReconnect to recover.
        new Thread(() -> {
            try {
                client.connect(opts);
                System.out.println("MQTT client connected to " + brokerUrl);
            } catch (Exception e) {
                System.err.println("MQTT connect failed: " + e.getMessage());
            }
        }, "mqtt-connector").start();

        return client;
    }
}
