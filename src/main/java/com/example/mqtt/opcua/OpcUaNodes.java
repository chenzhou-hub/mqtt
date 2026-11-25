package com.example.mqtt.opcua;

public final class OpcUaNodes {

    public static final String NAMESPACE_URI = "urn:springboot-mqtt:opcua-demo";
    public static final int NAMESPACE_INDEX = 2;

    public static final String TEMPERATURE_ID = "Demo.Temperature";
    public static final String HUMIDITY_ID = "Demo.Humidity";

    public static final String TEMPERATURE_NODE = "ns=" + NAMESPACE_INDEX + ";s=" + TEMPERATURE_ID;
    public static final String HUMIDITY_NODE = "ns=" + NAMESPACE_INDEX + ";s=" + HUMIDITY_ID;

    private OpcUaNodes() {
    }
}
