package io.vertx.mutiny.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.RedisOptions;

public class RedisClientTest {

    @Rule
    public GenericContainer<?> container = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testMutinyAPI() {
        Redis client = Redis.createClient(vertx, new RedisOptions()
                .setConnectionString("redis://" + container.getContainerIpAddress() + ":" + container.getMappedPort(6379)));
        RedisAPI redis = RedisAPI.api(client);

        Response object = redis.hset(Arrays.asList("book", "title", "The Hobbit"))
                .onItem().transformToUni(x -> redis.hgetall("book"))
                .subscribeAsCompletionStage()
                .join();
        assertThat(object.get("title").toString()).isEqualTo("The Hobbit");
    }
}
