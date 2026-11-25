package com.example.mqtt.opcua;

import com.example.mqtt.config.OpcUaProperties;
import org.eclipse.milo.opcua.sdk.server.EndpointConfig;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.transport.server.tcp.OpcTcpServerTransport;
import org.eclipse.milo.opcua.stack.transport.server.tcp.OpcTcpServerTransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class DemoOpcUaServer implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DemoOpcUaServer.class);

    private final OpcUaProperties properties;
    private OpcUaServer server;
    private DemoNamespace demoNamespace;

    public DemoOpcUaServer(OpcUaProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!properties.isServerEnabled()) {
            log.info("内嵌 OPC UA 模拟服务已关闭，使用外部服务: {}", properties.getEndpointUrl());
            return;
        }

        EndpointConfig endpoint = EndpointConfig.newBuilder()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindAddress("0.0.0.0")
                .setBindPort(properties.getServerPort())
                .setHostname("localhost")
                .setPath("/milo")
                .setSecurityPolicy(SecurityPolicy.None)
                .setSecurityMode(MessageSecurityMode.None)
                .addTokenPolicy(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS)
                .build();

        OpcUaServerConfig config = OpcUaServerConfig.builder()
                .setApplicationUri("urn:springboot-mqtt:opcua-demo-server")
                .setApplicationName(LocalizedText.english("Spring Boot MQTT OPC UA Demo Server"))
                .setProductUri("urn:springboot-mqtt:opcua-demo")
                .setEndpoints(Set.of(endpoint))
                .build();

        server = new OpcUaServer(config, profile -> new OpcTcpServerTransport(
                OpcTcpServerTransportConfig.newBuilder().build()));

        demoNamespace = new DemoNamespace(server);
        demoNamespace.startup();
        server.startup().get(10, TimeUnit.SECONDS);

        log.info("OPC UA 模拟服务已启动: opc.tcp://localhost:{}/milo", properties.getServerPort());
    }

    @Override
    public void destroy() throws Exception {
        if (demoNamespace != null) {
            demoNamespace.shutdown();
        }
        if (server != null) {
            server.shutdown().get(10, TimeUnit.SECONDS);
        }
    }
}
