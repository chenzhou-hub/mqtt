package com.example.mqtt.opcua;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.Lifecycle;
import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DemoNamespace extends ManagedNamespaceWithLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DemoNamespace.class);

    private final SubscriptionModel subscriptionModel;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "opcua-demo-data");
                t.setDaemon(true);
                return t;
            });

    private UaVariableNode temperatureNode;
    private UaVariableNode humidityNode;
    private double temperature = 23.5;
    private double humidity = 55.0;
    private int tick;

    DemoNamespace(OpcUaServer server) {
        super(server, OpcUaNodes.NAMESPACE_URI);

        subscriptionModel = new SubscriptionModel(server, this);
        getLifecycleManager().addLifecycle(subscriptionModel);
        getLifecycleManager().addStartupTask(this::createNodes);
        getLifecycleManager().addLifecycle(new Lifecycle() {
            @Override
            public void startup() {
                scheduler.scheduleAtFixedRate(DemoNamespace.this::updateValues, 0, 1, TimeUnit.SECONDS);
            }

            @Override
            public void shutdown() {
                scheduler.shutdownNow();
            }
        });
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    private void createNodes() {
        UaFolderNode folder = new UaFolderNode(
                getNodeContext(),
                newNodeId("Demo"),
                newQualifiedName("Demo"),
                LocalizedText.english("Demo Devices"));
        getNodeManager().addNode(folder);
        folder.addReference(new Reference(
                folder.getNodeId(), NodeIds.Organizes, NodeIds.ObjectsFolder.expanded(), false));

        temperatureNode = createVariable(folder, OpcUaNodes.TEMPERATURE_ID, temperature);
        humidityNode = createVariable(folder, OpcUaNodes.HUMIDITY_ID, humidity);
        log.info("OPC UA demo nodes created: {}, {}", OpcUaNodes.TEMPERATURE_NODE, OpcUaNodes.HUMIDITY_NODE);
    }

    private UaVariableNode createVariable(UaFolderNode folder, String id, double initialValue) {
        return UaVariableNode.build(getNodeContext(), builder -> {
            builder.setNodeId(newNodeId(id));
            builder.setBrowseName(newQualifiedName(id));
            builder.setDisplayName(LocalizedText.english(id));
            builder.setAccessLevel(AccessLevel.READ_WRITE);
            builder.setUserAccessLevel(AccessLevel.READ_WRITE);
            builder.setDataType(NodeIds.Double);
            builder.setTypeDefinition(NodeIds.BaseDataVariableType);
            builder.setMinimumSamplingInterval(100.0);
            builder.setValue(new DataValue(Variant.of(initialValue)));
            builder.addReference(new Reference(
                    builder.getNodeId(),
                    NodeIds.Organizes,
                    folder.getNodeId().expanded(),
                    Reference.Direction.INVERSE));
            return builder.buildAndAdd();
        });
    }

    private void updateValues() {
        if (temperatureNode == null || humidityNode == null) {
            return;
        }
        tick++;
        temperature = 23.5 + Math.sin(tick / 10.0) * 3;
        humidity = 55.0 + Math.cos(tick / 12.0) * 8;
        temperatureNode.setValue(new DataValue(Variant.of(temperature)));
        humidityNode.setValue(new DataValue(Variant.of(humidity)));
    }
}
