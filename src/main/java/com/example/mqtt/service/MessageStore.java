package com.example.mqtt.service;

import com.example.mqtt.config.MqttConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MessageStore {

    private static final Logger log = LoggerFactory.getLogger(MessageStore.class);
    private static final int MAX_MESSAGES = 50;

    private final Queue<ReceivedMessage> messages = new ConcurrentLinkedQueue<>();

    @ServiceActivator(inputChannel = MqttConfig.INBOUND_CHANNEL)
    public void receive(String payload,
                        @Header(MqttHeaders.RECEIVED_TOPIC) String topic,
                        @Header(MqttHeaders.RECEIVED_QOS) int qos) {
        messages.offer(new ReceivedMessage(topic, payload, qos, LocalDateTime.now()));
        while (messages.size() > MAX_MESSAGES) {
            messages.poll();
        }
        log.info("收到 MQTT 消息 topic={}, qos={}, payload={}", topic, qos, payload);
    }

    public List<ReceivedMessage> findAll() {
        return List.copyOf(messages);
    }

    public record ReceivedMessage(String topic, String payload, int qos, LocalDateTime receivedAt) {
    }
}
