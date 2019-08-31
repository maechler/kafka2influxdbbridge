FROM openjdk:11-jre-slim
ADD build/libs/iot-home.kafka2influxdbbridge-1.0.jar /opt/kafka2influxdbbridge/kafka2influxdbbridge.jar
WORKDIR /opt/kafka2influxdbbridge/
CMD java -jar kafka2influxdbbridge.jar