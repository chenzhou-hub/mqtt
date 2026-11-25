package com.example.mqtt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opcua")
public class OpcUaProperties {

    private boolean serverEnabled = true;
    private int serverPort = 12686;
    private String endpointUrl = "opc.tcp://localhost:12686/milo";
    private String topic = "opcua/demo/data";
    private double publishInterval = 1000.0;
    private double samplingInterval = 500.0;

    public boolean isServerEnabled() {
        return serverEnabled;
    }

    public void setServerEnabled(boolean serverEnabled) {
        this.serverEnabled = serverEnabled;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public double getPublishInterval() {
        return publishInterval;
    }

    public void setPublishInterval(double publishInterval) {
        this.publishInterval = publishInterval;
    }

    public double getSamplingInterval() {
        return samplingInterval;
    }

    public void setSamplingInterval(double samplingInterval) {
        this.samplingInterval = samplingInterval;
    }
}
