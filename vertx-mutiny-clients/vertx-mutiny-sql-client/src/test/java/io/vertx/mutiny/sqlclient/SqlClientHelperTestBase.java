package io.vertx.mutiny.sqlclient;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public abstract class SqlClientHelperTestBase {

    protected static final List<String> NAMES = Arrays.asList("John", "Paul", "Peter", "Andrew", "Peter", "Steven");

    protected static final String UNIQUE_NAMES_SQL = "select distinct firstname from folks order by firstname asc";

    protected static final String INSERT_FOLK_SQL = "insert into folks (firstname) values ('%s')";

    protected Pool pool;

    public void initDb() {
        pool.query("drop table if exists folks").executeAndAwait();
        pool.query("create table folks (firstname varchar(255) not null)").executeAndAwait();
        for (String name : NAMES) {
            pool.query(String.format(INSERT_FOLK_SQL, name)).executeAndAwait();
        }
    }

    protected void assertTableContainsInitDataOnly() throws Exception {
        List<String> actual = uniqueNames(pool).collectItems().asList().await().indefinitely();
        assertThat(actual).isEqualTo(NAMES.stream().sorted().distinct().collect(toList()));
    }

    protected static Multi<String> uniqueNames(SqlClient client) {
        return client.query(UNIQUE_NAMES_SQL).execute()
                .onItem().transformToMulti(rows -> Multi.createFrom().iterable(rows))
                .onItem().transform(row -> row.getString(0));
    }

    protected static Uni<Void> insertExtraFolks(SqlClient client) {
        return client.query(String.format(INSERT_FOLK_SQL, "Georges")).execute()
                .onItem().transformToUni(v -> client.query(String.format(INSERT_FOLK_SQL, "Henry")).execute())
                .onItem().ignore().andContinueWithNull();
    }

    protected static List<String> namesWithExtraFolks() {
        return Stream.concat(NAMES.stream(), Stream.of("Georges", "Henry")).sorted().distinct().collect(toList());
    }
}
