package io.vertx.mutiny.sqlclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.Test;

import io.smallrye.mutiny.Multi;

public abstract class InTransactionMultiTest extends SqlClientHelperTestBase {

    @Test
    public void inTransactionSuccess() throws Exception {
        List<String> actual = inTransaction(null).collectItems().asList().await().indefinitely();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(namesWithExtraFolks());
    }

    @Test
    public void inTransactionFailure() throws Exception {
        Exception failure = new Exception();
        List<String> emitted = Collections.synchronizedList(new ArrayList<>());
        try {
            inTransaction(failure).onItem().invoke(emitted::add).collectItems().asList().await().indefinitely();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(CompletionException.class).getCause().isEqualTo(failure);
            assertThat(emitted).containsExactlyInAnyOrderElementsOf(namesWithExtraFolks());
        }
        assertTableContainsInitDataOnly();
    }

    private Multi<String> inTransaction(Exception e) throws Exception {
        return SqlClientHelper.inTransactionMulti(pool, transaction -> {
            Multi<String> upstream = insertExtraFolks(transaction)
                    .onItem().transformToMulti(v -> uniqueNames(transaction));
            if (e == null) {
                return upstream;
            }
            return Multi.createBy().concatenating().streams(upstream, Multi.createFrom().failure(e));
        });
    }
}
