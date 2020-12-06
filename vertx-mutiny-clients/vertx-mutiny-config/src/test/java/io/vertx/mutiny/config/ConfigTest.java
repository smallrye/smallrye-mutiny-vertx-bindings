package io.vertx.mutiny.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;

public class ConfigTest {

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void test() {
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setConfig(new JsonObject().put("path", "my-config.json"));

        ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");
        ConfigRetriever retriever = ConfigRetriever.create(vertx,
                new ConfigRetrieverOptions().addStore(fileStore).addStore(sysPropsStore));
        JsonObject config = retriever.getConfigAndAwait();
        assertThat(config.getString("message")).isEqualTo("hello world!");
        assertThat(config.getString("java.io.tmpdir")).isNotBlank();
    }

}
