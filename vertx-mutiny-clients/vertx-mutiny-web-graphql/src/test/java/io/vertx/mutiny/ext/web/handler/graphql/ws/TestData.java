package io.vertx.mutiny.ext.web.handler.graphql.ws;

import static java.util.stream.Collectors.toList;

import java.util.*;

import io.vertx.core.json.JsonObject;

public class TestData {

    final Map<String, User> users;
    final List<Link> links;

    TestData() {
        User peter = new User(UUID.randomUUID().toString(), "Peter");
        User paul = new User(UUID.randomUUID().toString(), "Paul");
        User jack = new User(UUID.randomUUID().toString(), "Jack");

        Map<String, User> map = new HashMap<>();
        map.put(peter.getId(), peter);
        map.put(paul.getId(), paul);
        map.put(jack.getId(), jack);
        users = Collections.unmodifiableMap(map);

        List<Link> list = new ArrayList<>();
        list.add(new Link("https://vertx.io", "Vert.x project", peter.getId()));
        list.add(new Link("https://www.eclipse.org", "Eclipse Foundation", paul.getId()));
        list.add(new Link("http://reactivex.io", "ReactiveX libraries", jack.getId()));
        list.add(new Link("https://www.graphql-java.com", "GraphQL Java implementation", peter.getId()));
        links = Collections.unmodifiableList(list);
    }

    List<String> urls() {
        return links.stream().map(Link::getUrl).collect(toList());
    }

    boolean checkLinkUrls(List<String> expected, JsonObject body) {
        if (body.containsKey("errors")) {
            return false;
        }
        JsonObject data = body.getJsonObject("data");
        List<String> urls = data.getJsonArray("allLinks").stream()
                .map(JsonObject.class::cast)
                .map(json -> json.getString("url"))
                .collect(toList());
        return expected.equals(urls);
    }
}
