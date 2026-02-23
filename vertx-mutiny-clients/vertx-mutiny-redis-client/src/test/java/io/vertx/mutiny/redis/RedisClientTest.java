package io.vertx.mutiny.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Response;

class RedisClientTest {

    static GenericContainer<?> container = new GenericContainer<>("redis:6.2")
            .withExposedPorts(6379);

    private static Vertx vertx;

    @BeforeAll
    static void init() {
        container.start();
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @AfterAll
    static void cleanup() {
        vertx.closeAndAwait();
        container.stop();
    }

    @Test
    void testMutinyAPI() {
        Redis client = Redis.createClient(vertx, new RedisOptions()
                .setConnectionString("redis://" + container.getHost() + ":" + container.getMappedPort(6379)));
        RedisAPI redis = RedisAPI.api(client);

        Response object = redis.hset(Arrays.asList("book", "title", "The Hobbit"))
                .onItem().transformToUni(x -> redis.hgetall("book"))
                .subscribeAsCompletionStage()
                .join();
        assertThat(object.get("title").toString()).isEqualTo("The Hobbit");

        List<String> responses = redis.keys("*")
                .onItem().transformToMulti(r -> Multi.createFrom().iterable(r))
                .map(Response::toString)
                .collect().asList()
                .await().indefinitely();
        assertThat(responses).containsExactly("book");
    }
}
