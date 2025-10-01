package io.vertx.mutiny.db2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Db2Container;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.sqlclient.PoolOptions;

public class DB2ClientTest {

    public static Db2Container container = new Db2Container()
            .acceptLicense()
            .withReuse(true);

    @BeforeAll
    public static void init() {
        container.start();
    }

    @AfterAll
    public static void shutdown() {
        container.stop();
    }

    private Vertx vertx;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testMutinyAPI() {
        DB2ConnectOptions options = new DB2ConnectOptions()
                .setPort(container.getMappedPort(50000))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        Pool client = Pool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        RowSet<?> join = client.query("SELECT 1 FROM SYSIBM.SYSDUMMY1")
                .executeAndAwait();
        assertThat(join).isNotNull();
        assertThat(join.size()).isEqualTo(1);
    }

    @Test
    public void testSequence() {
        DB2ConnectOptions options = new DB2ConnectOptions()
                .setPort(container.getMappedPort(50000))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());

        Pool client = Pool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        Uni<Tuple2<RowSet<Row>, RowSet<Row>>> uni = client.getConnection()
                .flatMap(c -> Uni.combine().all().unis(
                        c.preparedQuery("SELECT 1 FROM SYSIBM.SYSDUMMY1").execute(),
                        c.preparedQuery("SELECT 1 FROM SYSIBM.SYSDUMMY1").execute()).asTuple());

        Tuple2<RowSet<Row>, RowSet<Row>> results = uni.await().indefinitely();
        assertThat(results).isNotNull();
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.getItem1()).isNotNull();
        assertThat(results.getItem2()).isNotNull();
    }

    @Test
    public void testTransaction() {
        DB2ConnectOptions options = new DB2ConnectOptions()
                .setPort(container.getMappedPort(50000))
                .setHost(container.getContainerIpAddress())
                .setDatabase(container.getDatabaseName())
                .setUser(container.getUsername())
                .setPassword(container.getPassword());
        Pool client = Pool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        Uni<Void> uni = client.getConnection()
                .onItem().transformToUni(c -> {
                    return c.begin()
                            .onItem().transformToUni(tx -> c
                                    .query("SELECT 1 FROM SYSIBM.SYSDUMMY1").execute()
                                    .call(() -> c.query("SELECT 1 FROM SYSIBM.SYSDUMMY1").execute())
                                    .onItem().transformToUni(results -> tx.commit())
                                    .onFailure().recoverWithUni(t -> tx.rollback()));
                });
        Void v = uni.await().indefinitely();
        assertThat(v).isNull();
    }
}
