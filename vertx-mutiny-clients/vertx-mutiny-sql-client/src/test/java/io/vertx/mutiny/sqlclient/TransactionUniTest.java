package io.vertx.mutiny.sqlclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.smallrye.mutiny.Uni;

public abstract class TransactionUniTest extends SqlClientHelperTestBase {

    @Test
    public void testCompletion() {
        AtomicBoolean committed = new AtomicBoolean();
        pool.getConnection()
                .onItem().transformToUni(c -> c.begin()
                        .onItem().invoke(tx -> tx.completion().subscribe().with(x -> committed.set(true)))
                        .onItem().transformToUni(Transaction::commit))
                .await().indefinitely();

        assertThat(committed).isTrue();

    }

    @Test
    public void inTransactionSuccess() {
        List<String> actual = inTransaction(null).await().indefinitely();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(namesWithExtraFolks());
    }

    @Test
    public void withTransactionSuccess() {
        List<String> actual = withTransaction(null).await().indefinitely();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(namesWithExtraFolks());
    }

    @Test
    public void inTransactionFailure() throws Exception {
        Exception failure = new Exception();
        try {
            inTransaction(failure).await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(CompletionException.class).getCause().isEqualTo(failure);
        }
        assertTableContainsInitDataOnly();
    }

    @Test
    public void withTransactionFailure() throws Exception {
        Exception failure = new Exception();
        try {
            withTransaction(failure).await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(CompletionException.class).getCause().isEqualTo(failure);
        }
        assertTableContainsInitDataOnly();
    }

    private Uni<List<String>> inTransaction(Exception e) {
        return SqlClientHelper.inTransactionUni(pool, transaction -> {
            Uni<List<String>> upstream = insertExtraFolks(transaction)
                    .onItem().transformToUni(v -> uniqueNames(transaction).collectItems().asList());
            if (e == null) {
                return upstream;
            }
            return upstream.onItem().transformToUni(v -> Uni.createFrom().failure(e));
        });
    }

    private Uni<List<String>> withTransaction(Exception e) {
        return pool.withTransaction(connection -> {
            Uni<List<String>> upstream = insertExtraFolks(connection)
                    .onItem().transformToUni(v -> uniqueNames(connection).collectItems().asList());
            if (e == null) {
                return upstream;
            }
            return upstream.onItem().transformToUni(v -> Uni.createFrom().failure(e));
        });
    }
}
