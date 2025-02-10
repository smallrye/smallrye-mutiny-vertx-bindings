package io.vertx.mutiny.sqlclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.smallrye.mutiny.Uni;

public abstract class TransactionUniTest extends SqlClientHelperTestBase {

//    @Test
//    public void testCompletion() {
//        AtomicBoolean committed = new AtomicBoolean();
//        pool.getConnection()
//                .onItem().transformToUni(c -> c.begin()
//                        .onItem().invoke(tx -> tx.completion().subscribe().with(x -> committed.set(true)))
//                        .onItem().transformToUni(Transaction::commit))
//                .await().indefinitely();
//
//        assertThat(committed).isTrue();
//
//    }
//
//    @Test
//    public void inTransactionSuccess() {
//        List<String> actual = inTransaction(false, null);
//        assertThat(actual).isEqualTo(namesWithExtraFolks());
//    }
//
//    @Test
//    public void withTransactionSuccess() {
//        List<String> actual = withTransaction(false, null);
//        assertThat(actual).isEqualTo(namesWithExtraFolks());
//    }
//
//    @Test
//    public void inTransactionUserFailure() throws Exception {
//        Exception failure = new Exception();
//        try {
//            inTransaction(true, failure);
//            fail("Exception expected");
//        } catch (Exception e) {
//            assertThat(e).isInstanceOf(CompletionException.class).getCause().isEqualTo(failure);
//        }
//        assertTableContainsInitDataOnly();
//    }
//
//    @Test
//    public void inTransactionDBFailure() throws Exception {
//        try {
//            inTransaction(true, null);
//            fail("Exception expected");
//        } catch (Exception e) {
//            verifyDuplicateException(e);
//        }
//        assertTableContainsInitDataOnly();
//    }
//
//    @Test
//    public void withTransactionUserFailure() throws Exception {
//        Exception failure = new Exception();
//        try {
//            withTransaction(true, failure);
//            fail("Exception expected");
//        } catch (Exception e) {
//            assertThat(e).isInstanceOf(CompletionException.class).getCause().isEqualTo(failure);
//        }
//        assertTableContainsInitDataOnly();
//    }
//
//    @Test
//    public void withTransactionDBFailure() throws Exception {
//        try {
//            withTransaction(true, null);
//            fail("Exception expected");
//        } catch (Exception e) {
//            verifyDuplicateException(e);
//        }
//        assertTableContainsInitDataOnly();
//    }
//
//    protected abstract void verifyDuplicateException(Exception e);
//
//    private List<String> inTransaction(boolean fail, Exception e) {
//        return SqlClientHelper.inTransactionUni(pool, transaction -> {
//            Uni<List<String>> upstream = insertExtraFolks(transaction)
//                    .onItem().transformToUni(v -> uniqueNames(transaction).collect().asList());
//            if (!fail) {
//                return upstream;
//            }
//            if (e != null) {
//                return upstream.onItem().transformToUni(v -> Uni.createFrom().failure(e));
//            }
//            return upstream.replaceWith(upstream);
//        }).await().indefinitely();
//    }
//
//    private List<String> withTransaction(boolean fail, Exception e) {
//        return pool.withTransaction(connection -> {
//            Uni<List<String>> upstream = insertExtraFolks(connection)
//                    .onItem().transformToUni(v -> uniqueNames(connection).collect().asList());
//            if (!fail) {
//                return upstream;
//            }
//            if (e != null) {
//                return upstream.onItem().transformToUni(v -> Uni.createFrom().failure(e));
//            }
//            return upstream.replaceWith(upstream);
//        }).await().indefinitely();
//    }
}
