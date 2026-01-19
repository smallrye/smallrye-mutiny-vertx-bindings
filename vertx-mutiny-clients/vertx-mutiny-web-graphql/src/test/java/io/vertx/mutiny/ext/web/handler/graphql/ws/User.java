package io.vertx.mutiny.ext.web.handler.graphql.ws;

/**
 * @author Thomas Segismont
 */
public class User {
    private final String id;
    private final String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}