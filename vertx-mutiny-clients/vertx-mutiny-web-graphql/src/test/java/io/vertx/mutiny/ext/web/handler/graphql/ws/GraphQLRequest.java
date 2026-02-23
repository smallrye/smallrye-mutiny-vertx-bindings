package io.vertx.mutiny.ext.web.handler.graphql.ws;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static java.util.stream.Collectors.joining;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.http.HttpClientAgent;
import io.vertx.mutiny.core.http.HttpClientResponse;
import io.vertx.mutiny.core.http.HttpHeaders;

/**
 * @author Thomas Segismont
 */
public class GraphQLRequest {
    static final String JSON = "application/json";
    static final String GRAPHQL = "application/graphql";

    private HttpMethod method = POST;
    private String graphQLQuery;
    private boolean graphQLQueryAsParam;
    private JsonObject variables = new JsonObject();
    private boolean variablesAsParam;
    private String contentType = JSON;

    GraphQLRequest setMethod(HttpMethod method) {
        this.method = method;
        if (method == GET) {
            graphQLQueryAsParam = variablesAsParam = true;
        } else if (method == POST) {
            graphQLQueryAsParam = variablesAsParam = false;
        }
        return this;
    }

    GraphQLRequest setGraphQLQuery(String graphQLQuery) {
        this.graphQLQuery = graphQLQuery;
        return this;
    }

    Uni<JsonObject> send(HttpClientAgent client) throws Exception {
        return send(client, 200);
    }

    Uni<JsonObject> send(HttpClientAgent client, int expectedStatus) throws Exception {
        return client
                .request(method, 8080, "localhost", getUri())
                .flatMap(request -> {
                    if (contentType != null) {
                        request.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
                    }
                    Buffer buffer;
                    if (GRAPHQL.equalsIgnoreCase(contentType)) {
                        buffer = graphQLQuery != null ? Buffer.buffer(graphQLQuery) : null;
                    } else {
                        buffer = getJsonBody();
                    }
                    Uni<HttpClientResponse> response;
                    if (buffer != null) {
                        response = request.send(buffer);
                    } else {
                        response = request.send();
                    }
                    return response.flatMap(res -> {
                        if (res.statusCode() == 500) {
                            return Uni.createFrom().failure(new Exception("failed to send"));
                        }
                        return res.body().map(JsonObject::new);
                    });
                });
    }

    private String getUri() {
        StringBuilder uri = new StringBuilder("/graphql");
        Map<String, String> params = new LinkedHashMap<>();
        if (graphQLQueryAsParam && graphQLQuery != null) {
            params.put("query", graphQLQuery);
        }
        if (variablesAsParam && !variables.isEmpty()) {
            params.put("variables", variables.encode());
        }
        if (!params.isEmpty()) {
            uri.append("?");
            uri.append(params.entrySet().stream()
                    .map(entry -> {
                        try {
                            return entry.getKey() + "=" + encode(entry.getValue());
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(joining("&")));
        }
        return uri.toString();
    }

    private Buffer getJsonBody() {
        JsonObject json = new JsonObject();
        if (graphQLQuery != null) {
            json.put("query", graphQLQuery);
        }
        if (!variables.isEmpty()) {
            json.put("variables", variables);
        }
        return json.isEmpty() ? null : json.toBuffer();
    }

    static String encode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
    }
}
