package io.smallrye.mutiny.vertx.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.Authenticator;

public class DummyStore {

    private final List<Authenticator> database = new ArrayList<>();

    public DummyStore add(Authenticator authenticator) {
        this.database.add(authenticator);
        return this;
    }

    public void clear() {
        database.clear();
    }

    public Uni<List<Authenticator>> fetch(Authenticator query) {
        return Uni.createFrom().item(
                database.stream()
                        .filter(entry -> {
                            if (query.getUserName() != null) {
                                return query.getUserName().equals(entry.getUserName());
                            }
                            if (query.getCredID() != null) {
                                return query.getCredID().equals(entry.getCredID());
                            }
                            // This is a bad query! both username and credID are null
                            return false;
                        })
                        .collect(Collectors.toList()));
    }

    public Uni<Void> store(Authenticator authenticator) {

        long updated = database.stream()
                .filter(entry -> authenticator.getCredID().equals(entry.getCredID()))
                .peek(entry -> {
                    // update existing counter
                    entry.setCounter(authenticator.getCounter());
                }).count();

        if (updated > 0) {
            return Uni.createFrom().nullItem();
        } else {
            database.add(authenticator);
            return Uni.createFrom().nullItem();
        }
    }
}
