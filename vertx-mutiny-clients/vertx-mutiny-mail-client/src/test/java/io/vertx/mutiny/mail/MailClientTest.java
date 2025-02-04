package io.vertx.mutiny.mail;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.mail.MailClient;

class MailClientTest {

    public static GenericContainer<?> container = new GenericContainer<>("mailhog/mailhog:latest")
            .withExposedPorts(1025);

    private static Vertx vertx;

    @BeforeAll
    static void init() {
        container.start();
        vertx = Vertx.vertx();
        assertThat(vertx, is(notNullValue()));

    }

    @AfterAll
    static void shutdown() {
        vertx.closeAndAwait();
        container.stop();
    }

    @Test
    void testMutinyAPI() {
        MailClient client = MailClient.createShared(vertx, new MailConfig()
                .setPort(container.getMappedPort(1025))
                .setHostname(container.getHost()));
        assertThat(client, is(notNullValue()));
        client.sendMail(new MailMessage().setText("hello mutiny")
                .setSubject("test email")
                .setTo("clement@apache.org")
                .setFrom("clement@apache.org"))
                .subscribeAsCompletionStage()
                .join();
    }

    @Test
    void testBlockingAPI() {
        MailClient client = MailClient.createShared(vertx, new MailConfig()
                .setPort(container.getMappedPort(1025))
                .setHostname(container.getHost()));
        assertThat(client, is(notNullValue()));
        client.sendMailAndAwait(new MailMessage().setText("hello mutiny")
                .setSubject("test email")
                .setTo("clement@apache.org")
                .setFrom("clement@apache.org"));
    }
}
