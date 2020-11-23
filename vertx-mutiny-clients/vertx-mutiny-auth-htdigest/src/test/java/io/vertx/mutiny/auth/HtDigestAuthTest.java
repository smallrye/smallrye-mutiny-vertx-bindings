package io.vertx.mutiny.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.User;
import io.vertx.mutiny.ext.auth.htdigest.HtdigestAuth;

public class HtDigestAuthTest {

    private Vertx vertx;
    private HtdigestAuth authProvider;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
        authProvider = HtdigestAuth.create(vertx);
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testValidDigestWithQOP() {
        JsonObject authInfo = new JsonObject()
                .put("method", "GET")

                .put("username", "Mufasa")
                .put("realm", "testrealm@host.com")
                .put("nonce", "dcd98b7102dd2f0e8b11d0f600bfb0c093")
                .put("uri", "/dir/index.html")
                .put("qop", "auth")
                .put("nc", "00000001")
                .put("cnonce", "0a4f113b")
                .put("response", "6629fae49393a05397450978507c4ef1")
                .put("opaque", "5ccc069c403ebaf9f0171e9517f40e41");

        User user = authProvider.authenticate(authInfo).await().indefinitely();
        assertNotNull(user);
        assertEquals("Mufasa", user.principal().getString("username"));
    }

    @Test
    public void testValidDigestWithoutQOP() {
        JsonObject authInfo = new JsonObject()
                .put("method", "GET")

                .put("username", "Mufasa")
                .put("realm", "testrealm@host.com")
                .put("nonce", "dcd98b7102dd2f0e8b11d0f600bfb0c093")
                .put("uri", "/dir/index.html")
                .put("nc", "00000001")
                .put("cnonce", "0a4f113b")
                .put("response", "670fd8c2df070c60b045671b8b24ff02")
                .put("opaque", "5ccc069c403ebaf9f0171e9517f40e41");

        User user = authProvider.authenticate(authInfo).await().indefinitely();
        assertNotNull(user);
        assertNotNull(user);
        assertEquals("Mufasa", user.principal().getString("username"));
    }

}
