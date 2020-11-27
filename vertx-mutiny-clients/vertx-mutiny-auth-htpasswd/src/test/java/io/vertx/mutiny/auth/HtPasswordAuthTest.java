package io.vertx.mutiny.auth;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.htpasswd.HtpasswdAuthOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.User;
import io.vertx.mutiny.ext.auth.authentication.Credentials;
import io.vertx.mutiny.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.mutiny.ext.auth.htpasswd.HtpasswdAuth;

public class HtPasswordAuthTest {

    private Vertx vertx;
    private HtpasswdAuth authProviderCrypt;
    private HtpasswdAuth authProviderPlainText;
    private HtpasswdAuth authProviderUsersAreAuthorizedForNothing;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
        authProviderCrypt = HtpasswdAuth.create(vertx);
        authProviderPlainText = HtpasswdAuth.create(vertx, new HtpasswdAuthOptions().setPlainTextEnabled(true));
        authProviderUsersAreAuthorizedForNothing = HtpasswdAuth.create(vertx);
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void md5() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("md5", "myPassword");
        User user = authProviderCrypt.authenticate(Credentials.newInstance(credentials)).await().indefinitely();
        assertNotNull(user);
    }

    @Test
    public void sha1() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("sha1", "myPassword");
        User user = authProviderCrypt.authenticate(Credentials.newInstance(credentials)).await().indefinitely();
        assertNotNull(user);
    }

    @Test
    public void crypt() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("crypt", "myPassword");
        User user = authProviderCrypt.authenticate(Credentials.newInstance(credentials)).await().indefinitely();
        assertNotNull(user);
    }

    @Test
    public void plaintext() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("plaintext", "myPassword");
        User user = authProviderPlainText.authenticate(Credentials.newInstance(credentials)).await().indefinitely();
        assertNotNull(user);
    }

    @Test
    public void authzFalse() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("md5", "myPassword");
        User user = authProviderUsersAreAuthorizedForNothing.authenticate(Credentials.newInstance(credentials)).await()
                .indefinitely();
        assertNotNull(user);
        assertFalse(PermissionBasedAuthorization.create("something").match(user));
    }

}
