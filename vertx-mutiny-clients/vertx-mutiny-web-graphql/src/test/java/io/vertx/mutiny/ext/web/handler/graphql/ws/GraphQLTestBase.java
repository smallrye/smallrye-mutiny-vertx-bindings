package io.vertx.mutiny.ext.web.handler.graphql.ws;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static java.util.stream.Collectors.toList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.handler.graphql.GraphQLHandlerOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClientAgent;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.handler.graphql.GraphQLHandler;

/**
 * @author Thomas Segismont
 */
public class GraphQLTestBase {

    protected TestData testData = new TestData();
    protected GraphQLHandler graphQLHandler;

    protected Vertx vertx;
    protected HttpClientAgent client;
    protected HttpServer server;
    protected Router router;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        server = vertx.createHttpServer(getHttpServerOptions().setMaxFormFields(2048));
        client = vertx.createHttpClient(getHttpClientOptions());
        graphQLHandler = GraphQLHandler.create(graphQL(), createOptions());
        router.route("/graphql").order(100).handler(graphQLHandler);
        server.requestHandler(router).listenAndAwait();
    }

    @AfterEach
    public void clean() {
        vertx.closeAndAwait();
    }

    protected HttpServerOptions getHttpServerOptions() {
        return new HttpServerOptions().setPort(8080).setHost("localhost");
    }

    protected HttpClientOptions getHttpClientOptions() {
        return new HttpClientOptions().setDefaultPort(8080);
    }

    protected GraphQLHandlerOptions createOptions() {
        return new GraphQLHandlerOptions();
    }

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
                .build();
    }

    protected Object getAllLinks(DataFetchingEnvironment env) {
        boolean secureOnly = env.getArgument("secureOnly");
        return testData.links.stream()
                .filter(link -> !secureOnly || link.getUrl().startsWith("https://"))
                .map(link -> {
                    if (env.getRoot() != null) {
                        return new Link(link.getUrl(), env.getRoot().toString(), link.getUserId());
                    } else {
                        return link;
                    }
                })
                .collect(toList());
    }
}
