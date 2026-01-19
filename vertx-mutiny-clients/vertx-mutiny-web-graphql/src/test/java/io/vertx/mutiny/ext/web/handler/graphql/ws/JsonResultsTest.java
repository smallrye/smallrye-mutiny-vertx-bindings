package io.vertx.mutiny.ext.web.handler.graphql.ws;

import static graphql.schema.idl.RuntimeWiring.*;
import static io.vertx.core.http.HttpMethod.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.instrumentation.JsonObjectAdapter;

/**
 * @author Thomas Segismont
 */
public class JsonResultsTest extends GraphQLTestBase {

    @Override
    public GraphQL graphQL() {
        String schema = vertx.fileSystem().readFileBlocking("links.graphqls").toString();

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("allLinks", this::getAllLinks))
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        return GraphQL.newGraphQL(graphQLSchema)
                .instrumentation(new JsonObjectAdapter())
                .build();
    }

    @Override
    protected Object getAllLinks(DataFetchingEnvironment env) {
        @SuppressWarnings("unchecked")
        List<Link> links = (List<Link>) super.getAllLinks(env);
        return links.stream()
                .map(link -> {
                    JsonObject jsonObject = new JsonObject()
                            .put("url", link.getUrl())
                            .put("description", link.getDescription())
                            .put("userId", link.getUserId());
                    return jsonObject;
                })
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
    }

    @Test
    public void testSimpleGet() throws Exception {
        GraphQLRequest request = new GraphQLRequest()
                .setMethod(GET)
                .setGraphQLQuery("query { allLinks { url } }");
        request.send(client)
                .onItem().invoke(body -> {
                    assertTrue(testData.checkLinkUrls(testData.urls(), body));
                }).await().indefinitely();
    }

    @Test
    public void testSimplePost() throws Exception {
        GraphQLRequest request = new GraphQLRequest()
                .setMethod(POST)
                .setGraphQLQuery("query { allLinks { url } }");
        request.send(client)
                .onItem().invoke(body -> {
                    assertTrue(testData.checkLinkUrls(testData.urls(), body));
                }).await().indefinitely();
    }
}
