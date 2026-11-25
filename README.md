# springboot-mqtt

基于 Spring Boot 4.1.2 + Spring Integration MQTT 的 MQTT 收发 Demo（Java 17），并内置 **OPC UA → MQTT 网关**示例。

## 技术栈

- Spring Boot 4.1.2
- Spring Integration MQTT 7.1.x
- Eclipse Paho MQTT v3 客户端 1.2.5
- Eclipse Milo 1.1.8（OPC UA Client + Server，Java 17+）
- 默认连接公共测试 broker `tcp://broker.emqx.io:1883`

## 启动

```bash
mvn spring-boot:run
```

应用启动后会同时：

1. 连接 MQTT Broker 并订阅 `mqtt.topics`
2. 启动内嵌 OPC UA 模拟服务（端口 `12686`），产生温度/湿度数据
3. 作为 OPC UA Client 订阅这两个节点，并把变化数据发布到 MQTT topic `opcua/demo/data`

查看转发过来的数据：

```bash
curl http://localhost:8080/api/mqtt/messages
```

返回示例：

```json
[
  {
    "topic": "opcua/demo/data",
    "payload": "{\"node\":\"ns=2;s=Demo.Temperature\",\"value\":24.3,\"time\":\"...\"}",
    "qos": 1,
    "receivedAt": "..."
  }
]
```

## 配置

常用配置在 `application.yml`：

```yaml
mqtt:
  broker-url: tcp://broker.emqx.io:1883   # 本地 Mosquitto 可改为 tcp://localhost:1883
  topics:
    - springboot/mqtt/demo
    - opcua/demo/data

opcua:
  server-enabled: true        # false 则只连接外部 OPC UA Server
  server-port: 12686
  endpoint-url: opc.tcp://localhost:12686/milo
  topic: opcua/demo/data
  publish-interval: 1000      # 订阅发布周期(ms)
  sampling-interval: 500      # 节点采样周期(ms)
```

本地 Broker 可用 Docker 启动：

```bash
docker run -d --name mosquitto -p 1883:1883 eclipse-mosquitto:2
```

## MQTT 接口

发布消息（不传 `topic` 时使用 `mqtt.default-topic`）：

```bash
curl -X POST http://localhost:8080/api/mqtt/publish \
  -H "Content-Type: application/json" \
  -d "{\"topic\":\"springboot/mqtt/demo\",\"message\":\"hello mqtt\"}"
```

查看本应用收到的消息（最近 50 条）：

```bash
curl http://localhost:8080/api/mqtt/messages
```

## 关键类

- `config/MqttConfig`：MQTT 客户端工厂、入站/出站适配器
- `gateway/MqttGateway`：MQTT 发布网关
- `service/MessageStore`：接收消息内存缓存
- `opcua/DemoOpcUaServer`：内嵌 OPC UA 模拟服务
- `opcua/DemoNamespace`：温度/湿度节点与定时更新
- `opcua/OpcUaToMqttBridge`：订阅 OPC UA 节点并转发到 MQTT
