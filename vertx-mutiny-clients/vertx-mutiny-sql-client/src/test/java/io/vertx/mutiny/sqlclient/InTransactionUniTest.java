package io.vertx.mutiny.sqlclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.Test;

import io.smallrye.mutiny.Uni;

public abstract class InTransactionUniTest extends SqlClientHelperTestBase {

    @Test
    public void inTransactionSuccess() throws Exception {
        List<String> actual = inTransaction(null).await().indefinitely();
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

    private Uni<List<String>> inTransaction(Exception e) throws Exception {
        return SqlClientHelper.inTransactionUni(pool, transaction -> {
            Uni<List<String>> upstream = insertExtraFolks(transaction)
                    .onItem().transformToUni(v -> uniqueNames(transaction).collectItems().asList());
            if (e == null) {
                return upstream;
            }
            return upstream.onItem().transformToUni(v -> Uni.createFrom().failure(e));
        });
    }
}
