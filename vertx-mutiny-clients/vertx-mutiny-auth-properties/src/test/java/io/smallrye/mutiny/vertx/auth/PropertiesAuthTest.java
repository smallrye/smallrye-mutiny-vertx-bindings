package io.smallrye.mutiny.vertx.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.User;
import io.vertx.mutiny.ext.auth.authorization.AuthorizationContext;
import io.vertx.mutiny.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.mutiny.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.mutiny.ext.auth.authorization.WildcardPermissionBasedAuthorization;
import io.vertx.mutiny.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.mutiny.ext.auth.properties.PropertyFileAuthorization;

public class PropertiesAuthTest {

    private Vertx vertx;
    private PropertyFileAuthentication authn;
    private PropertyFileAuthorization authz;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        authn = PropertyFileAuthentication
                .create(vertx, this.getClass().getResource("/test-auth.properties").getFile());
        authz = PropertyFileAuthorization.create(vertx, this.getClass().getResource("/test-auth.properties").getFile());
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testSimpleAuthenticate() {
        UsernamePasswordCredentials authInfo = new UsernamePasswordCredentials("tim", "sausages");
        User user = authn.authenticate(authInfo).await().indefinitely();
        assertNotNull(user);
    }

    @Test
    public void testSimpleAuthenticateFailWrongPassword() {
        UsernamePasswordCredentials authInfo = new UsernamePasswordCredentials("tim", "wrongpassword");
        try {
            authn.authenticate(authInfo).await().indefinitely();
        } catch (CompletionException e) {
            assertTrue(e.getMessage().contains("invalid username/password"));
        }
    }

    @Test
    public void testSimpleAuthenticateFailWrongUser() {
        UsernamePasswordCredentials authInfo = new UsernamePasswordCredentials("tim", "sausages");
        try {
            authn.authenticate(authInfo).await().indefinitely();
        } catch (CompletionException e) {
            assertTrue(e.getMessage().contains("unknown user"));
        }
    }

    @Test
    public void testHasRole() {
        login()
                .onItem().transformToUni(user -> authz.getAuthorizations(user)
                        .onItem().invoke(() -> assertTrue(
                                RoleBasedAuthorization.create("morris_dancer").match(AuthorizationContext.create(user)))))
                .await().indefinitely();
    }

    private Uni<User> login() {
        UsernamePasswordCredentials authInfo = new UsernamePasswordCredentials("tim", "sausages");
        return authn.authenticate(authInfo);
    }

    @Test
    public void testNotHasRole() {
        login()
                .onItem().transformToUni(user -> authz.getAuthorizations(user)
                        .onItem()
                        .invoke(() -> assertFalse(
                                RoleBasedAuthorization.create("manager").match(AuthorizationContext.create(user)))))
                .await().indefinitely();
    }

    @Test
    public void testHasPermission() {
        login()
                .onItem().transformToUni(user -> authz.getAuthorizations(user)
                        .onItem().invoke(() -> assertTrue(PermissionBasedAuthorization.create("do_actual_work")
                                .match(AuthorizationContext.create(user)))))
                .await().indefinitely();
    }

    @Test
    public void testNotHasPermission() {
        login()
                .onItem().transformToUni(user -> authz.getAuthorizations(user)
                        .onItem().invoke(() -> assertFalse(
                                PermissionBasedAuthorization.create("play_golf")
                                        .match(AuthorizationContext.create(user)))))
                .await().indefinitely();
    }

    @Test
    public void testHasWildcardPermission() {
        UsernamePasswordCredentials authInfo = new UsernamePasswordCredentials("paulo", "secret");
        User user = authn.authenticate(authInfo).await().indefinitely();

        assertNotNull(user);

        authz.getAuthorizations(user).await().indefinitely();
        assertTrue(
                WildcardPermissionBasedAuthorization.create("do_actual_work")
                        .match(AuthorizationContext.create(user)));
    }

    @Test
    public void testHasWildcardMatchPermission() {
        UsernamePasswordCredentials authInfo = new UsernamePasswordCredentials("editor", "secret");
        User user = authn.authenticate(authInfo).await().indefinitely();

        assertNotNull(user);

        authz.getAuthorizations(user).await().indefinitely();
        assertTrue(
                WildcardPermissionBasedAuthorization.create("newsletter:edit:13")
                        .match(AuthorizationContext.create(user)));
    }
}
