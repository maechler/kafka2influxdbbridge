package ch.maechler.iothome;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.*;
import org.apache.logging.log4j.*;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Kafka2InfluxDbBridge {
    private final Logger logger = LogManager.getLogger(Kafka2InfluxDbBridge.class);
    private KafkaConsumer<String, String> kafkaConsumer;
    private InfluxDB influxDB;

    public static void main(String[] args) {
        new Kafka2InfluxDbBridge().run();
    }

    private void run() {
        logger.info("Start to run Kafka2InfluxDbBridge.");

        var kafkaConsumerProperties = new Properties();
        var clientId = Optional.ofNullable(System.getenv("CLIENT_ID")).orElse("Kafka2InfluxDbBridge");
        var kafkaBootstrapServers = Optional.ofNullable(System.getenv("KAFKA_BOOTSTRAP_SERVERS")).orElse("localhost:9092");
        var kafkaTopic = Optional.ofNullable(System.getenv("KAFKA_TOPIC")).orElse(".*");
        var kafkaPollTimeout = Integer.parseInt(Optional.ofNullable(System.getenv("KAFKA_POLL_TIMEOUT")).orElse("1000"));
        var influxdbUrl = Optional.ofNullable(System.getenv("INFLUXDB_URL")).orElse("http://localhost:8086");
        var influxdbUser = Optional.ofNullable(System.getenv("INFLUXDB_USER")).orElse("root");
        var influxdbPassword = Optional.ofNullable(System.getenv("INFLUXDB_PASSWORD")).orElse("root");
        var influxdbDatabaseName = Optional.ofNullable(System.getenv("INFLUXDB_DATABASE_NAME")).orElse("default_database");
        var influxdbDatabaseDuration = Optional.ofNullable(System.getenv("INFLUXDB_DATABASE_DURATION")).orElse("365d");

        logger.info(
            "Configuration values: \n CLIENT_ID={} \n KAFKA_BOOTSTRAP_SERVERS={} \n KAFKA_TOPIC={} \n KAFKA_POLL_TIMEOUT={} \n INFLUXDB_URL={} \n INFLUXDB_USER={} \n INFLUXDB_PASSWORD={} \n INFLUXDB_DATABASE_NAME={} \n INFLUXDB_DATABASE_DURATION={}",
                clientId, kafkaBootstrapServers, kafkaTopic, kafkaPollTimeout, influxdbUrl, influxdbUser, "****", influxdbDatabaseName, influxdbDatabaseDuration
        );

        kafkaConsumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        kafkaConsumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, clientId);
        kafkaConsumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaConsumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        kafkaConsumerProperties.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 2000);

        try {
            logger.info("Connecting to InfluxDB and Kafka broker.");

            influxDB = InfluxDBFactory.connect(influxdbUrl, influxdbUser, influxdbPassword);
            createInfluxDatabase(influxdbDatabaseName, influxdbDatabaseDuration);
            influxDB.setDatabase(influxdbDatabaseName);

            kafkaConsumer = new KafkaConsumer<>(kafkaConsumerProperties);
            kafkaConsumer.subscribe(Pattern.compile(kafkaTopic));

            while (true) {
                var consumerRecords = kafkaConsumer.poll(kafkaPollTimeout);

                consumerRecords.forEach(record -> {
                    logger.info("Record: {}, {}, {}, {}, {}", record.topic(), record.key(), record.value(), record.partition(), record.offset());

                    JsonElement recordRoot = new JsonParser().parse(record.value());
                    var topicParts = record.topic().split("\\.");
                    var measurement = topicParts[1];
                    var field = topicParts[2];
                    var point = Point.measurement(measurement)
                            .time(recordRoot.getAsJsonObject().get("timestamp").getAsLong(), TimeUnit.MILLISECONDS)
                            .addField(field, recordRoot.getAsJsonObject().get("value").getAsFloat())
                            .build();

                    influxDB.write(point);
                });

                influxDB.close();
                kafkaConsumer.commitAsync();
            }
        } catch(Exception e) {
            logger.error("Oops, an error occurred.", e);

            if (influxDB != null) influxDB.close();
            if (kafkaConsumer != null) kafkaConsumer.close();
        }
    }

    private void createInfluxDatabase(String influxdbDatabaseName, String influxdbDatabaseDuration) {
        if (!influxDB.describeDatabases().contains(influxdbDatabaseName)) {
            // Bound queries (prepared statements) do not yet work here
            influxDB.query(new Query("CREATE DATABASE " + influxdbDatabaseName + " with duration " + influxdbDatabaseDuration + ";"));
        }
    }
}
