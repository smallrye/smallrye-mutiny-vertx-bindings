package io.vertx.mutiny.uritemplate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class UriTemplateTest {

    /**
     * Example taken from https://en.wikipedia.org/wiki/URI_Template.
     */
    @Test
    public void test() {
        String string = UriTemplate.of("http://example.com/people/{firstName}-{lastName}/SSN")
                .expandToString(Variables.variables().set("firstName", "foo").set("lastName", "bar"));
        assertThat(string).isEqualTo("http://example.com/people/foo-bar/SSN");
    }
}
