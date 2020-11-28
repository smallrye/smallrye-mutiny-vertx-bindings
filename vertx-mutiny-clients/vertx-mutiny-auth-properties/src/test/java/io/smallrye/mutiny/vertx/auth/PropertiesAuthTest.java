package io.smallrye.mutiny.vertx.auth;

import static org.junit.Assert.*;

import java.util.concurrent.CompletionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
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

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        authn = PropertyFileAuthentication
                .create(vertx, this.getClass().getResource("/test-auth.properties").getFile());
        authz = PropertyFileAuthorization.create(vertx, this.getClass().getResource("/test-auth.properties").getFile());
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testSimpleAuthenticate() {
        JsonObject authInfo = new JsonObject().put("username", "tim").put("password", "sausages");
        User user = authn.authenticate(authInfo).await().indefinitely();
        assertNotNull(user);
    }

    @Test
    public void testSimpleAuthenticateFailWrongPassword() {
        JsonObject authInfo = new JsonObject().put("username", "tim").put("password", "wrongpassword");
        try {
            authn.authenticate(authInfo).await().indefinitely();
        } catch (CompletionException e) {
            assertTrue(e.getMessage().contains("invalid username/password"));
        }
    }

    @Test
    public void testSimpleAuthenticateFailWrongUser() {
        JsonObject authInfo = new JsonObject().put("username", "frank").put("password", "sausages");
        try {
            authn.authenticate(authInfo).await().indefinitely();
        } catch (CompletionException e) {
            assertTrue(e.getMessage().contains("invalid username/password"));
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
        JsonObject authInfo = new JsonObject().put("username", "tim").put("password", "sausages");
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
        JsonObject authInfo = new JsonObject().put("username", "paulo").put("password", "secret");
        User user = authn.authenticate(authInfo).await().indefinitely();

        assertNotNull(user);

        authz.getAuthorizations(user).await().indefinitely();
        assertTrue(
                WildcardPermissionBasedAuthorization.create("do_actual_work")
                        .match(AuthorizationContext.create(user)));
    }

    @Test
    public void testHasWildcardMatchPermission() {
        JsonObject authInfo = new JsonObject().put("username", "editor").put("password", "secret");
        User user = authn.authenticate(authInfo).await().indefinitely();

        assertNotNull(user);

        authz.getAuthorizations(user).await().indefinitely();
        assertTrue(
                WildcardPermissionBasedAuthorization.create("newsletter:edit:13")
                        .match(AuthorizationContext.create(user)));
    }
}
