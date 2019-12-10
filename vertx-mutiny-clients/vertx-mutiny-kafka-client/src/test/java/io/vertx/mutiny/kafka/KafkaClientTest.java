package io.vertx.mutiny.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumer;
import io.vertx.mutiny.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.mutiny.kafka.client.producer.KafkaProducer;
import io.vertx.mutiny.kafka.client.producer.KafkaProducerRecord;

public class KafkaClientTest {

    @Rule
    public KafkaContainer container = new KafkaContainer();

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testMutinyAPI() {
        Map<String, String> configOfTheConsumer = new HashMap<>();
        configOfTheConsumer.put("bootstrap.servers", container.getBootstrapServers());
        configOfTheConsumer.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configOfTheConsumer.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configOfTheConsumer.put("group.id", "my_group");
        configOfTheConsumer.put("auto.offset.reset", "earliest");
        configOfTheConsumer.put("enable.auto.commit", "false");

        Map<String, String> configOfProducer = new HashMap<>();
        configOfProducer.put("bootstrap.servers", container.getBootstrapServers());
        configOfProducer.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        configOfProducer.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        configOfProducer.put("acks", "1");

        String uuid = UUID.randomUUID().toString();
        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, configOfTheConsumer,
                String.class, String.class);

        Uni<String> first = consumer.toMulti()
                .onItem().apply(KafkaConsumerRecord::value)
                .collectItems().first();

        consumer.subscribe("my-topic").await().indefinitely();

        KafkaProducer<String, String> producer = KafkaProducer.create(vertx, configOfProducer);
        producer.write(KafkaProducerRecord.create("my-topic", uuid)).await().indefinitely();

        Optional<String> optional = first.await().asOptional().indefinitely();
        assertThat(optional).contains(uuid);
    }
}
