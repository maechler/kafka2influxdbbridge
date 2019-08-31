# Kafka to InfluxDB Bridge

This is an extremely primitive Kafka to InfluxDB bridge, consisting of around 100 lines of code. It is not optimized for extensibility, performance nor stability and should not be used in a production environment.

## Configuration

| Environment Variable | Default value | Description |
|---|---|---|
| CLIENT_ID | `Kafka2InfluxDbBridge` | Client ID used to connect to MQTT and Kafka broker. |
| KAFKA_BOOTSTRAP_SERVERS | `localhost:9092` | Host and port of your Kafka broker. |
| KAFKA_TOPIC | `.*` | The topic we subscribe to given as RegEx. |
| KAFKA_POLL_TIMEOUT | `1000` | The time, in milliseconds, spent waiting in poll if data is not available in the buffer. |
| INFLUXDB_URL | `http://localhost:8086` | URL to the InfluxDB. |
| INFLUXDB_USER | `root` | InfluxDB user that is used. |
| INFLUXDB_PASSWORD | `root` | InfluxDB password that is used. |
| INFLUXDB_DATABASE_NAME | `default_database` | The name of the database that will be created and used. |
| INFLUXDB_DATABASE_DURATION | `365d` | The default policy duration for the database. |

## Build

`./gradlew build`

## Run

`java -jar build/libs/iot-home.kafka2influxdbbridge-1.0.jar`

### Run with custom configuration

```bash 
export KAFKA_BOOTSTRAP_SERVERS=kafka:9092;
export INFLUXDB_URL=http://influxdb:8086;
java -jar build/libs/iot-home.kafka2influxdbbridge-1.0.jar;
```

## Docker

**dockerhub:** https://hub.docker.com/r/marmaechler/kafka2influxdbbridge 

### Docker compose example

```yaml
version: "3"
services:
  kafka2influxdbbridge:
  image: marmaechler/kafka2influxdbbridge:latest
  depends_on:
    - kafka
    - influxdb
  restart: always
  environment:
    KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    INFLUXDB_URL: http://influxdb:8086
    INFLUXDB_DATABASE_NAME: iot_home
```
