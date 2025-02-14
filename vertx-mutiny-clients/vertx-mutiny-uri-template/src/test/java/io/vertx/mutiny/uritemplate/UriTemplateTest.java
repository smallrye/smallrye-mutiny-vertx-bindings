package io.vertx.mutiny.uritemplate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.uritemplate.UriTemplate;
import io.vertx.uritemplate.Variables;

public class UriTemplateTest {

    /**
     * Example taken from https://en.wikipedia.org/wiki/URI_Template.
     */
    @Test
    public void test() {
        String string = UriTemplate.of("http://example.com/people/{firstName}-{lastName}/SSN")
                .expandToString(Variables.variables().set("firstName", "foo").set("lastName", "bar"));
        Assertions.assertThat(string).isEqualTo("http://example.com/people/foo-bar/SSN");
    }

}
