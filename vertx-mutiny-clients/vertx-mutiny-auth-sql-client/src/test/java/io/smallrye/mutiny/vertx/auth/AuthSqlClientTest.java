package io.smallrye.mutiny.vertx.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.User;
import io.vertx.mutiny.ext.auth.authentication.AuthenticationProvider;
import io.vertx.mutiny.ext.auth.authorization.AuthorizationProvider;
import io.vertx.mutiny.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.mutiny.ext.auth.sqlclient.SqlAuthentication;
import io.vertx.mutiny.ext.auth.sqlclient.SqlAuthorization;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

public class AuthSqlClientTest {

    public static GenericContainer<?> container = new GenericContainer<>("mysql:8")
            .withEnv("MYSQL_USER", "mysql")
            .withEnv("MYSQL_PASSWORD", "password")
            .withEnv("MYSQL_ROOT_PASSWORD", "password")
            .withEnv("MYSQL_DATABASE", "testschema")
            .withExposedPorts(3306)
            .withClasspathResourceMapping("mysql-auth-ddl-test.sql", "/docker-entrypoint-initdb.d/init.sql",
                    BindMode.READ_ONLY);

    private Vertx vertx;
    private Pool mysql;

    @BeforeEach
    public void setUp() {
        container.start();
        vertx = Vertx.vertx();
        mysql = Pool.pool(
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

    @AfterEach
    public void tearDown() {
        mysql.closeAndAwait();
        vertx.closeAndAwait();
        container.stop();
    }

    @Test
    public void testAuthenticate() {
        AuthenticationProvider authn = SqlAuthentication.create(mysql);

        var creds = new UsernamePasswordCredentials("lopus", "secret");
        User user = authn.authenticate(creds).await().indefinitely();
        Assertions.assertNotNull(user);
        Assertions.assertEquals(creds.getUsername(), user.principal().getString("username"));
    }

    @Test
    public void testAuthenticateBadPassword() {
        AuthenticationProvider authn = SqlAuthentication.create(mysql);

        var creds = new UsernamePasswordCredentials("lopus", "s3cr3t");

        try {
            authn.authenticate(creds).await().indefinitely();
        } catch (Exception e) {
            Assertions.assertEquals("Invalid username/password", e.getMessage());
        }
    }

    @Test
    public void testAuthenticateBadUser() {
        AuthenticationProvider authn = SqlAuthentication.create(mysql);

        var creds = new UsernamePasswordCredentials("lopes", "s3cr3t");
        try {
            authn.authenticate(creds).await().indefinitely();
        } catch (Exception e) {
            Assertions.assertEquals("Invalid username/password", e.getMessage());
        }
    }

    @Test
    public void testAuthoriseHasRole() {
        var creds = new UsernamePasswordCredentials("lopus", "secret");

        AuthenticationProvider authn = SqlAuthentication.create(mysql);

        User user = authn.authenticate(creds).await().indefinitely();
        Assertions.assertNotNull(user);
        AuthorizationProvider authz = SqlAuthorization.create(mysql);
        authz.getAuthorizations(user).await().indefinitely();
        Assertions.assertTrue(RoleBasedAuthorization.create("dev").match(user));
    }
}
