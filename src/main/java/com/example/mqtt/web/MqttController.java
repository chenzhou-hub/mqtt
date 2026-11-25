package com.example.mqtt.web;

import com.example.mqtt.config.MqttProperties;
import com.example.mqtt.gateway.MqttGateway;
import com.example.mqtt.service.MessageStore;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    private final MqttGateway mqttGateway;
    private final MessageStore messageStore;
    private final MqttProperties properties;

    public MqttController(MqttGateway mqttGateway, MessageStore messageStore, MqttProperties properties) {
        this.mqttGateway = mqttGateway;
        this.messageStore = messageStore;
        this.properties = properties;
    }

    @PostMapping("/publish")
    public Map<String, Object> publish(@RequestBody PublishRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message 不能为空");
        }
        String topic = StringUtils.hasText(request.topic()) ? request.topic() : properties.getDefaultTopic();
        mqttGateway.publish(request.message(), topic);
        return Map.of("published", true, "topic", topic, "message", request.message());
    }

    @GetMapping("/messages")
    public List<MessageStore.ReceivedMessage> messages() {
        return messageStore.findAll();
    }

    public record PublishRequest(String topic, String message) {
    }
}
