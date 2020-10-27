package io.vertx.mutiny.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class PostGreSQLClientTest {

    @Rule
    public PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

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
        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        Pool client = PgPool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        RowSet<?> join = client.query("SELECT 1")
                .executeAndAwait();
        assertThat(join).isNotNull();
        assertThat(join.size()).isEqualTo(1);
    }

    @Test
    public void testSequence() {
        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        Pool client = PgPool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        Uni<Tuple2<RowSet<Row>, RowSet<Row>>> uni = client.getConnection()
                .flatMap(c -> c.preparedQuery("SELECT 1").execute()
                        .and(c.preparedQuery("SELECT 1").execute()));

        Tuple2<RowSet<Row>, RowSet<Row>> results = uni.await().indefinitely();
        assertThat(results).isNotNull();
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.getItem1()).isNotNull();
        assertThat(results.getItem2()).isNotNull();
    }

    @Test
    public void testTransaction() {
        PgConnectOptions options = new PgConnectOptions()
                .setPort(container.getMappedPort(5432))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());
        Pool client = PgPool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        Uni<Void> uni = client
                .getConnection()
                .flatMap(c -> c.begin()
                        .flatMap(tx -> c
                                .query("SELECT 1").execute()
                                .call(() -> c.query("SELECT").execute())
                                .onItem().transformToUni(results -> tx.commit())
                                .onFailure().recoverWithUni(t -> tx.rollback())));

        Void v = uni.await().indefinitely();
        assertThat(v).isNull();
    }
}
