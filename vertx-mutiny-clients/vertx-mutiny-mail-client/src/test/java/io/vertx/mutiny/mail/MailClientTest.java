package io.vertx.mutiny.mail;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.mail.MailClient;

public class MailClientTest {

    @Rule
    public GenericContainer container = new GenericContainer("digiplant/fake-smtp")
            .withExposedPorts(25)
            .withFileSystemBind("target", "/tmp/fakemail", BindMode.READ_WRITE);

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx, is(notNullValue()));
    }

    @After
    public void tearDown() {
        vertx.closeAndAwait();
    }

    @Test
    public void testMutinyAPI() {
        MailClient client = MailClient.createShared(vertx, new MailConfig()
                .setPort(container.getMappedPort(25))
                .setHostname(container.getContainerIpAddress()));
        assertThat(client, is(notNullValue()));
        client.sendMail(new MailMessage().setText("hello mutiny")
                .setSubject("test email")
                .setTo("clement@apache.org")
                .setFrom("clement@apache.org"))
                .subscribeAsCompletionStage()
                .join();
    }

    @Test
    public void testBlockingAPI() {
        MailClient client = MailClient.createShared(vertx, new MailConfig()
                .setPort(container.getMappedPort(25))
                .setHostname(container.getContainerIpAddress()));
        assertThat(client, is(notNullValue()));
        client.sendMailAndAwait(new MailMessage().setText("hello mutiny")
                .setSubject("test email")
                .setTo("clement@apache.org")
                .setFrom("clement@apache.org"));
    }
}
