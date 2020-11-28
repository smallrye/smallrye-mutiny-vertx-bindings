package io.smallrye.mutiny.vertx.auth;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.User;
import io.vertx.mutiny.ext.auth.authentication.AuthenticationProvider;
import io.vertx.mutiny.ext.auth.authorization.AuthorizationProvider;
import io.vertx.mutiny.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.mutiny.ext.auth.sqlclient.SqlAuthentication;
import io.vertx.mutiny.ext.auth.sqlclient.SqlAuthorization;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class AuthSqlClientTest {

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("mysql:5.7")
            .withEnv("MYSQL_USER", "mysql")
            .withEnv("MYSQL_PASSWORD", "password")
            .withEnv("MYSQL_ROOT_PASSWORD", "password")
            .withEnv("MYSQL_DATABASE", "testschema")
            .withExposedPorts(3306)
            .withClasspathResourceMapping("mysql-auth-ddl-test.sql", "/docker-entrypoint-initdb.d/init.sql",
                    BindMode.READ_ONLY);

    private Vertx vertx;
    private MySQLPool mysql;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        mysql = MySQLPool.pool(
                vertx,
                new MySQLConnectOptions()
                        .setPort(container.getMappedPort(3306))
                        .setHost(container.getContainerIpAddress())
                        .setDatabase("testschema")
                        .setUser("mysql")
                        .setPassword("password"),
                new PoolOptions()
                        .setMaxSize(5));
    }

    @After
    public void tearDown() {
        mysql.closeAndAwait();
        vertx.closeAndAwait();
    }

    @Test
    public void testAuthenticate() {
        AuthenticationProvider authn = SqlAuthentication.create(mysql);

        JsonObject authInfo = new JsonObject();
        authInfo.put("username", "lopus").put("password", "secret");

        User user = authn.authenticate(authInfo).await().indefinitely();
        assertNotNull(user);
        assertEquals("lopus", user.principal().getString("username"));
    }

    @Test
    public void testAuthenticateBadPassword() {
        AuthenticationProvider authn = SqlAuthentication.create(mysql);

        JsonObject authInfo = new JsonObject();
        authInfo.put("username", "lopus").put("password", "s3cr3t");

        try {
            authn.authenticate(authInfo).await().indefinitely();
        } catch (Exception e) {
            assertEquals("Invalid username/password", e.getCause().getMessage());
        }
    }

    @Test
    public void testAuthenticateBadUser() {
        AuthenticationProvider authn = SqlAuthentication.create(mysql);

        JsonObject authInfo = new JsonObject();
        authInfo.put("username", "lopes").put("password", "s3cr3t");
        try {
            authn.authenticate(authInfo).await().indefinitely();
        } catch (Exception e) {
            assertEquals("Invalid username/password", e.getCause().getMessage());
        }
    }

    @Test
    public void testAuthoriseHasRole() {
        JsonObject authInfo = new JsonObject();
        authInfo.put("username", "lopus").put("password", "secret");

        AuthenticationProvider authn = SqlAuthentication.create(mysql);

        User user = authn.authenticate(authInfo).await().indefinitely();
        assertNotNull(user);
        AuthorizationProvider authz = SqlAuthorization.create(mysql);
        authz.getAuthorizations(user).await().indefinitely();
        assertTrue(RoleBasedAuthorization.create("dev").match(user));
    }
}
