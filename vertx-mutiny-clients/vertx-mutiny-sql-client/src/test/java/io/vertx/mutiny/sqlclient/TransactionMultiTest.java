package io.vertx.mutiny.sqlclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.junit.Test;

import io.smallrye.mutiny.Multi;

public abstract class TransactionMultiTest extends SqlClientHelperTestBase {

//    List<String> emitted = Collections.synchronizedList(new ArrayList<>());
//
//    @Test
//    public void inTransactionSuccess() throws Exception {
//        inTransaction(false, null);
//        assertThat(emitted).isEqualTo(namesWithExtraFolks());
//    }
//
//    @Test
//    public void inTransactionUserFailure() throws Exception {
//        Exception failure = new Exception();
//        try {
//            inTransaction(true, failure);
//        } catch (Exception e) {
//            assertThat(e).isInstanceOf(CompletionException.class).getCause().isEqualTo(failure);
//            assertThat(emitted).isEqualTo(namesWithExtraFolks());
//        }
//        assertTableContainsInitDataOnly();
//    }
//
//    @Test
//    public void inTransactionDBFailure() throws Exception {
//        try {
//            inTransaction(true, null);
//        } catch (Exception e) {
//            verifyDuplicateException(e);
//            assertThat(emitted).isEqualTo(namesWithExtraFolks());
//        }
//        assertTableContainsInitDataOnly();
//    }
//
//    protected abstract void verifyDuplicateException(Exception e);
//
//    private void inTransaction(boolean fail, Exception e) throws Exception {
//        SqlClientHelper.inTransactionMulti(pool, transaction -> {
//            Multi<String> upstream = insertExtraFolks(transaction)
//                    .onItem().transformToMulti(v -> uniqueNames(transaction));
//            if (!fail) {
//                return upstream;
//            }
//            if (e != null) {
//                return Multi.createBy().concatenating().streams(upstream, Multi.createFrom().failure(e));
//            }
//            return Multi.createBy().concatenating().streams(upstream, upstream);
//        }).onItem().invoke(emitted::add).collect().last().await().indefinitely();
//    }
}
