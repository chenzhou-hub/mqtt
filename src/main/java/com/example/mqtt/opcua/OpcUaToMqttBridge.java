package com.example.mqtt.opcua;

import com.example.mqtt.config.OpcUaProperties;
import com.example.mqtt.gateway.MqttGateway;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@DependsOn("demoOpcUaServer")
public class OpcUaToMqttBridge implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(OpcUaToMqttBridge.class);

    private final OpcUaProperties properties;
    private final MqttGateway mqttGateway;

    private OpcUaClient client;
    private OpcUaSubscription subscription;

    public OpcUaToMqttBridge(OpcUaProperties properties, MqttGateway mqttGateway) {
        this.properties = properties;
        this.mqttGateway = mqttGateway;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        client = OpcUaClient.create(
                properties.getEndpointUrl(),
                endpoints -> endpoints.stream()
                        .filter(e -> SecurityPolicy.None.getUri().equals(e.getSecurityPolicyUri()))
                        .findFirst(),
                transportConfig -> {
                },
                config -> config
                        .setApplicationName(LocalizedText.english("springboot-mqtt opcua bridge"))
                        .setApplicationUri("urn:springboot-mqtt:opcua-bridge")
                        .setIdentityProvider(new AnonymousProvider()));

        client.connect();
        log.info("OPC UA 客户端已连接: {}", properties.getEndpointUrl());

        subscription = new OpcUaSubscription(client, properties.getPublishInterval());
        subscription.create();

        addMonitoredItem(OpcUaNodes.TEMPERATURE_ID);
        addMonitoredItem(OpcUaNodes.HUMIDITY_ID);

        try {
            subscription.synchronizeMonitoredItems();
        } catch (MonitoredItemSynchronizationException e) {
            log.warn("部分 OPC UA 监控项创建失败", e);
        }
        log.info("OPC UA 数据将转发到 MQTT topic: {}", properties.getTopic());
    }

    private void addMonitoredItem(String nodeId) {
        OpcUaMonitoredItem item = OpcUaMonitoredItem.newDataItem(
                new NodeId(OpcUaNodes.NAMESPACE_INDEX, nodeId));
        item.setSamplingInterval(properties.getSamplingInterval());
        item.setDataValueListener((monitoredItem, value) ->
                forward(monitoredItem.getReadValueId().getNodeId(), value));
        subscription.addMonitoredItem(item);
    }

    private void forward(NodeId nodeId, DataValue value) {
        Object raw = value.value() == null ? null : value.value().getValue();
        String valueJson = raw instanceof Number
                ? raw.toString()
                : "\"" + String.valueOf(raw).replace("\"", "\\\"") + "\"";
        String payload = String.format(
                "{\"node\":\"%s\",\"value\":%s,\"time\":\"%s\"}",
                nodeId.toParseableString().replace("\"", "\\\""), valueJson, Instant.now());
        mqttGateway.publish(payload, properties.getTopic());
    }

    @Override
    public void destroy() throws Exception {
        if (subscription != null) {
            subscription.delete();
        }
        if (client != null) {
            client.disconnect();
        }
    }
}
