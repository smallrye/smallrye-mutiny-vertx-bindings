package io.smallrye.mutiny.vertx.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletionException;

import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.ldap.LdapAuthenticationOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.User;
import io.vertx.mutiny.ext.auth.ldap.LdapAuthentication;

@CreateDS(name = "myDS", partitions = { @CreatePartition(name = "test", suffix = "dc=myorg,dc=com") })
@CreateLdapServer(transports = { @CreateTransport(protocol = "LDAP", address = "localhost") })
@ApplyLdifFiles({ "ldap.ldif" })
@ExtendWith(ApacheDSTestExtension.class)
public class LdapAuthTest {

    private Vertx vertx;

    // Note: all the next fields are required by ApacheDSTestExtension
    public static DirectoryService classDirectoryService;
    public static DirectoryService methodDirectoryService;
    public static DirectoryService directoryService;
    public static LdapServer classLdapServer;
    public static LdapServer methodLdapServer;
    public static LdapServer ldapServer;
    public static LdapConnectionTemplate ldapConnectionTemplate;
    public static long revision = 0L;

    private LdapAuthentication provider;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        LdapAuthenticationOptions ldapOptions = new LdapAuthenticationOptions()
                .setUrl("ldap://localhost:" + ldapServer.getPort())
                .setAuthenticationQuery("uid={0},ou=Users,dc=myorg,dc=com");

        provider = LdapAuthentication.create(vertx, ldapOptions);
    }

    @AfterEach
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testSimpleAuthenticate() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("tim", "sausages");
        User user = provider.authenticate(credentials).await().indefinitely();
        assertNotNull(user);
    }

    @Test
    public void testSimpleAuthenticateFailWrongPassword() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("tim", "wrongpassword");
        try {
            provider.authenticate(credentials).await().indefinitely();
        } catch (CompletionException e) {
            assertTrue(e.getMessage().contains("INVALID_CREDENTIALS"));
        }
    }

    @Test
    public void testSimpleAuthenticateFailWrongUser() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("frank", "sausages");
        try {
            provider.authenticate(credentials).await().indefinitely();
        } catch (CompletionException e) {
            assertTrue(e.getMessage().contains("INVALID_CREDENTIALS"));
        }
    }
}
