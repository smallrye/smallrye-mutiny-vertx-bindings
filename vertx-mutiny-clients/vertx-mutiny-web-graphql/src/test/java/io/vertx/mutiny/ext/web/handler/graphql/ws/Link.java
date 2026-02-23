package io.vertx.mutiny.ext.web.handler.graphql.ws;

public class Link {
    private final String url;
    private final String description;
    private final String userId;

    public Link(String url, String description, String userId) {
        this.url = url;
        this.description = description;
        this.userId = userId;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public String getUserId() {
        return userId;
    }
}