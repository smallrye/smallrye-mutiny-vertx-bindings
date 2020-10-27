package io.vertx.mutiny.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.impl.jose.JWK;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.User;
import io.vertx.mutiny.ext.auth.jwt.JWTAuth;

public class JwtAuthTest {

    private Vertx vertx;
    private static final String JWT_VALID = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJQYXVsbyIsImV4cCI6MTc0NzA1NTMxMywiaWF0IjoxNDMxNjk1MzEzLCJwZXJtaXNzaW9ucyI6WyJyZWFkIiwid3JpdGUiLCJleGVjdXRlIl0sInJvbGVzIjpbImFkbWluIiwiZGV2ZWxvcGVyIiwidXNlciJdfQ.UdA6oYDn9s_k7uogFFg8jvKmq9RgITBnlq4xV6JGsCY";

    @Before
    public void setup() {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testAuthentication() {
        JWTAuth authProvider = JWTAuth.create(vertx, getConfig());
        JsonObject authInfo = new JsonObject().put("jwt", JWT_VALID);
        User user = authProvider.authenticate(authInfo).await().indefinitely();
        assertNotNull(user);
        Assert.assertEquals("Paulo", user.principal().getString("sub"));
    }

    @Test
    public void testGenerateNewToken() {
        JsonObject payload = new JsonObject()
                .put("sub", "Paulo")
                .put("exp", 1747055313)
                .put("iat", 1431695313)
                .put("permissions", new JsonArray()
                        .add("read")
                        .add("write")
                        .add("execute"))
                .put("roles", new JsonArray()
                        .add("admin")
                        .add("developer")
                        .add("user"));
        JWTAuth authProvider = JWTAuth.create(vertx, getConfig());
        String token = authProvider.generateToken(payload, new JWTOptions().setSubject("Paulo"));
        assertNotNull(token);
        assertEquals(JWT_VALID, token);
    }

    private JWTAuthOptions getConfig() {
        return new JWTAuthOptions()
                .setKeyStore(new KeyStoreOptions()
                        .setPath("keystore.jceks")
                        .setType("jceks")
                        .setPassword("secret"));
    }

    @Test
    public void publicRSA() {
        JsonObject jwk = new JsonObject()
                .put("kty", "RSA")
                .put("n",
                        "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw")
                .put("e", "AQAB")
                .put("alg", "RS256")
                .put("kid", "2011-04-29");

        new JWK(jwk);
    }
}
