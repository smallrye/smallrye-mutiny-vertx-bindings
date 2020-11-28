package io.smallrye.mutiny.vertx.auth.shiro;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.shiro.PropertiesProviderConstants;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.User;
import io.vertx.mutiny.ext.auth.shiro.ShiroAuth;

public class ShiroTest {

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testAuthentication() {
        JsonObject config = new JsonObject();
        config.put(PropertiesProviderConstants.PROPERTIES_PROPS_PATH_FIELD, "classpath:test-auth.properties");
        ShiroAuth provider = ShiroAuth
                .create(vertx, new ShiroAuthOptions().setType(ShiroAuthRealmType.PROPERTIES).setConfig(config));

        JsonObject authInfo = new JsonObject().put("username", "tim").put("password", "sausages");
        User user = provider.authenticate(authInfo).await().indefinitely();

        assertNotNull(user);
        assertEquals(user.principal().getString("username"), "tim");
        assertTrue(user.attributes().isEmpty());

        authInfo = new JsonObject().put("username", "paulo").put("password", "secret");
        user = provider.authenticate(authInfo).await().indefinitely();
        assertNotNull(user);
        assertTrue(user.isAuthorized("do_actual_work").await().indefinitely());
    }
}
