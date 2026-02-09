package io.smallrye.mutiny.vertx.apigenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class NullableParameterTest {

    @Test
    void testNullablePlainParameter() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Foo", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;

                @VertxGen
                public interface Foo {
                    @Nullable String foo(@Nullable String s);
                    String bar(String s);

                    static Foo create() {
                        return new Foo() {
                            @Override
                            public String foo(String s) {
                                return s;
                            }

                            @Override
                            public String bar(String s) {
                                return s.toUpperCase();
                            }
                        };
                    }
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        var shimClass = env.getClass("org.acme.mutiny.Foo");
        assertThat(shimClass).isNotNull();
        var instance = env.invoke(shimClass, "create");
        assertThat(instance).isNotNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(String.class, null))).isNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(String.class, "foo"))).isEqualTo("foo");
        assertThat((String) env.invoke(instance, "bar", Tuple2.of(String.class, "bar"))).isEqualTo("BAR");
        assertThatThrownBy(() -> env.invoke(instance, "bar", Tuple2.of(String.class, null)))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void testNullableVertxGenParameter() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Refed", """
                package org.acme;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Refed {
                    void m();

                    static Refed create() {
                        return new Refed() {
                            public void m() {
                                // do nothing
                            }
                        };
                    }
                }
                """);
        env.addJavaCode("org.acme", "Foo", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;

                @VertxGen
                public interface Foo {
                    @Nullable Refed foo(@Nullable Refed s);
                    Refed bar(Refed s);

                    static Foo create() {
                        return new Foo() {
                            @Override
                            public Refed foo(Refed s) {
                                return s;
                            }

                            @Override
                            public Refed bar(Refed s) {
                                return s;
                            }
                        };
                    }
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        var instance = createInstance("org.acme.mutiny.Foo", env);
        var refed = createInstance("org.acme.mutiny.Refed", env);
        assertThat(instance).isNotNull();
        assertThat((Object) env.invoke(instance, "foo", Tuple2.of(refed.getClass(), null))).isNull();
        assertThat((Object) env.invoke(instance, "foo", Tuple2.of(refed.getClass(), refed))).isEqualTo(refed);
        assertThat((Object) env.invoke(instance, "bar", Tuple2.of(refed.getClass(), refed))).isEqualTo(refed);
        assertThatThrownBy(() -> env.invoke(instance, "bar", Tuple2.of(refed.getClass(), null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void testNullableHandlerParameter() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Refed", """
                package org.acme;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Refed {
                    String m();

                    static Refed create() {
                        return new Refed() {
                            public String m() {
                                return "ok";
                            }
                        };
                    }
                }
                """);
        env.addJavaCode("org.acme", "Foo", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;
                import io.vertx.core.Handler;

                @VertxGen
                public interface Foo {
                    @Nullable String foo(@Nullable Handler<Refed> h);
                    String bar(Handler<Refed> h);

                    @Nullable String a(@Nullable Handler<String> h);
                    String b(Handler<String> h);

                    static Foo create() {
                        return new Foo() {
                            @Override
                            public String foo(Handler<Refed> s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    s.handle(Refed.create());
                                    return "ok";
                            }

                            @Override
                            public String bar(Handler<Refed> s) {
                                    // NPE if s is null - not nullable.
                                    s.handle(Refed.create());
                                    return "ok";
                            }

                            @Override
                            public String a(Handler<String> s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    s.handle("foo");
                                    return "ok";
                            }

                            @Override
                            public String b(Handler<String> s) {
                                    // NPE if s is null - not nullable.
                                    s.handle("foo");
                                    return "ok";
                            }
                        };
                    }
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        var instance = createInstance("org.acme.mutiny.Foo", env);
        Consumer handler = x -> {
        };
        assertThat(instance).isNotNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Consumer.class, null))).isNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Consumer.class, handler))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "bar", Tuple2.of(Consumer.class, handler))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "bar", Tuple2.of(Consumer.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        assertThat((String) env.invoke(instance, "a", Tuple2.of(Consumer.class, null))).isNull();
        assertThat((String) env.invoke(instance, "a", Tuple2.of(Consumer.class, handler))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "b", Tuple2.of(Consumer.class, handler))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "b", Tuple2.of(Consumer.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    // Test with Handler<Void> -> Runnable
    void testNullableRunnableParameter() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Foo", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;
                import io.vertx.core.Handler;

                @VertxGen
                public interface Foo {
                    @Nullable String foo(@Nullable Handler<Void> h);
                    String bar(Handler<Void> h);

                    static Foo create() {
                        return new Foo() {
                            @Override
                            public String foo(Handler<Void> s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    s.handle(null);
                                    return "ok";
                            }

                            @Override
                            public String bar(Handler<Void> s) {
                                    // NPE if s is null - not nullable.
                                    s.handle(null);
                                    return "ok";
                            }
                        };
                    }
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        var instance = createInstance("org.acme.mutiny.Foo", env);
        Runnable handler = () -> {
            // Do nothing.
        };
        assertThat(instance).isNotNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Runnable.class, null))).isNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Runnable.class, handler))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "bar", Tuple2.of(Runnable.class, handler))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "bar", Tuple2.of(Runnable.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void testNullableFunctionParameter() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Refed", """
                package org.acme;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Refed {
                    String m();

                    static Refed create() {
                        return new Refed() {
                            public String m() {
                                return "ok";
                            }
                        };
                    }
                }
                """);
        env.addJavaCode("org.acme", "Foo", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;
                import io.vertx.core.Handler;
                import java.util.function.Function;
                import io.vertx.core.Future;

                @VertxGen
                public interface Foo {
                    // Functions returning a Vertx Gen type, and consuming a plain type
                    @Nullable String a(@Nullable Function<String, Refed> h);
                    String b(Function<String, Refed> h);
                    // Functions consuming a Vertx Gen type, and returning a plain type
                    @Nullable String c(@Nullable Function<Refed, String> h);
                    String d(Function<Refed, String> h);
                    // Functions consuming a Vertx Gen type, and returning a Vertx Gen type
                    @Nullable String e(@Nullable Function<Refed, Refed> h);
                    String f(Function<Refed, Refed> h);
                    // Plain function
                    @Nullable String g(@Nullable Function<String, String> h);
                    String h(Function<String, String> h);
                    // Function taking a plain type and returning a future of plain type
                    @Nullable String i(@Nullable Function<String, Future<String>> h);
                    String j(Function<String, Future<String>> h);
                    // Function taking a plain type and returning a future of Vert.x Gen
                    @Nullable String k(@Nullable Function<String, Future<Refed>> h);
                    String l(Function<String, Future<Refed>> h);
                    // Function taking a Vertx Gen type and returning a future of Vert.x Gen
                    @Nullable String m(@Nullable Function<Refed, Future<Refed>> h);
                    String n(Function<Refed, Future<Refed>> h);
                    // Function taking a Vertx Gen type and returning a future of plain
                    @Nullable String o(@Nullable Function<Refed, Future<String>> h);
                    String p(Function<Refed, Future<String>> h);

                    static Foo create() {
                        return new Foo() {
                            // Functions returning a Vertx Gen type, and consuming a plain type
                            public String a(Function<String, Refed> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.apply("foo").m();
                            }
                            public String b(Function<String, Refed> h) {
                                return h.apply("foo").m();
                            }

                            // Functions consuming a Vertx Gen type, and returning a plain type
                            public String c(Function<Refed, String> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.apply(Refed.create());
                            }
                            public String d(Function<Refed, String> h) {
                                return h.apply(Refed.create());
                            }

                            // Functions consuming a Vertx Gen type, and returning a Vertx Gen type
                            public String e(Function<Refed, Refed> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.apply(Refed.create()).m();
                            }
                            public String f(Function<Refed, Refed> h) {
                                return h.apply(Refed.create()).m();
                            }

                            // Plain function
                            public String g(@Nullable Function<String, String> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.apply("foo");
                            }
                            public String h(Function<String, String> h) {
                                return h.apply("foo");
                            }

                            // Function taking a plain type and returning a future of plain type
                            public String i(@Nullable Function<String, Future<String>> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.apply("foo").result();
                            }
                            public String j(Function<String, Future<String>> h) {
                                return h.apply("foo").result();
                            }

                            // Function taking a plain type and returning a future of Vert.x Gen
                            public String k(@Nullable Function<String, Future<Refed>> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.apply("foo").result().m();
                            }
                            public String l(Function<String, Future<Refed>> h) {
                                return h.apply("foo").result().m();
                            }

                            // Function taking a Vertx Gen type and returning a future of Vert.x Gen
                            public String m(Function<Refed, Future<Refed>> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.apply(Refed.create()).result().m();
                            }
                            public String n(Function<Refed, Future<Refed>> h) {
                                return h.apply(Refed.create()).result().m();
                            }

                            // Function taking a Vertx Gen type and returning a future of plain
                            public String o(Function<Refed, Future<String>> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.apply(Refed.create()).result();
                            }
                            public String p(Function<Refed, Future<String>> h) {
                                return h.apply(Refed.create()).result();
                            }
                        };
                    }
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        var instance = createInstance("org.acme.mutiny.Foo", env);
        assertThat(instance).isNotNull();

        // Functions returning a Vertx Gen type, and consuming a plain type
        // @Nullable String a(@Nullable Function<String, Refed> h);
        // String b(Function<String, Refed> h);
        Function function = x -> createInstance("org.acme.mutiny.Refed", env);
        assertThat((String) env.invoke(instance, "a", Tuple2.of(Function.class, null))).isNull();
        assertThat((String) env.invoke(instance, "a", Tuple2.of(Function.class, function))).isEqualTo("ok");

        assertThat((String) env.invoke(instance, "b", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "b", Tuple2.of(Function.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        // Functions consuming a Vertx Gen type, and returning a plain type
        //  @Nullable String c(@Nullable Function<Refed, String> h);
        //  String d(Function<Refed, String> h);
        function = x -> env.invoke(x, "m");
        assertThat((String) env.invoke(instance, "c", Tuple2.of(Function.class, null))).isNull();
        assertThat((String) env.invoke(instance, "c", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "d", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "d", Tuple2.of(Function.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        // Functions consuming a Vertx Gen type, and returning a Vertx Gen type
        // @Nullable String e(@Nullable Function<Refed, Refed> h);
        // String f(Function<Refed, Refed> h);
        function = x -> createInstance("org.acme.mutiny.Refed", env);
        assertThat((String) env.invoke(instance, "e", Tuple2.of(Function.class, null))).isNull();
        assertThat((String) env.invoke(instance, "e", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "f", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "f", Tuple2.of(Function.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        // Plain function
        // @Nullable String g(@Nullable Function<String, String> h);
        // String h(Function<String, String> h);
        function = x -> ((String) x).toUpperCase();
        assertThat((String) env.invoke(instance, "g", Tuple2.of(Function.class, null))).isNull();
        assertThat((String) env.invoke(instance, "g", Tuple2.of(Function.class, function))).isEqualTo("FOO");
        assertThat((String) env.invoke(instance, "h", Tuple2.of(Function.class, function))).isEqualTo("FOO");
        assertThatThrownBy(() -> env.invoke(instance, "h", Tuple2.of(Function.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        // Function taking a plain type and returning a future of plain type
        // @Nullable String i(@Nullable Function<String, Future<String>> h);
        // String j(Function<String, Future<String>> h);
        function = x -> Uni.createFrom().item((String) x);
        assertThat((String) env.invoke(instance, "i", Tuple2.of(Function.class, null))).isNull();
        assertThat((String) env.invoke(instance, "i", Tuple2.of(Function.class, function))).isEqualTo("foo");
        assertThat((String) env.invoke(instance, "j", Tuple2.of(Function.class, function))).isEqualTo("foo");
        assertThatThrownBy(() -> env.invoke(instance, "j", Tuple2.of(Function.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        // Function taking a plain type and returning a future of Vert.x Gen
        // @Nullable String k(@Nullable Function<String, Future<Refed>> h);
        // String l(Function<String, Future<Refed>> h);
        function = x -> Uni.createFrom().item(createInstance("org.acme.mutiny.Refed", env));
        assertThat((String) env.invoke(instance, "k", Tuple2.of(Function.class, null))).isNull();
        assertThat((String) env.invoke(instance, "k", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "l", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "l", Tuple2.of(Function.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        // Function taking a Vertx Gen type and returning a future of Vert.x Gen
        // @Nullable String m(@Nullable Function<Refed, Future<Refed>> h);
        // String n(Function<Refed, Future<Refed>> h);
        function = x -> Uni.createFrom().item(x);
        assertThat((String) env.invoke(instance, "m", Tuple2.of(Function.class, null))).isNull();
        assertThat((String) env.invoke(instance, "m", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "n", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "n", Tuple2.of(Function.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        // Function taking a Vertx Gen type and returning a future of plain
        // @Nullable String o(@Nullable Function<Refed, Future<String>> h);
        // String p(Function<Refed, Future<String>> h);
        function = x -> Uni.createFrom().item("ok");
        assertThat((String) env.invoke(instance, "o", Tuple2.of(Function.class, null))).isNull();
        assertThat((String) env.invoke(instance, "o", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "p", Tuple2.of(Function.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "p", Tuple2.of(Function.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

    }

    @Test
    void testNullableConsumerParameter() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Refed", """
                package org.acme;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Refed {
                    String m();

                    static Refed create() {
                        return new Refed() {
                            public String m() {
                                return "ok";
                            }
                        };
                    }
                }
                """);
        env.addJavaCode("org.acme", "Foo", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;
                import java.util.function.Consumer;

                @VertxGen
                public interface Foo {
                    @Nullable String foo(@Nullable Consumer<Refed> h);
                    String bar(Consumer<Refed> h);

                    @Nullable String a(@Nullable Consumer<String> h);
                    String b(Consumer<String> h);

                    static Foo create() {
                        return new Foo() {
                            @Override
                            public String foo(Consumer<Refed> s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    s.accept(Refed.create());
                                    return "ok";
                            }

                            @Override
                            public String bar(Consumer<Refed> s) {
                                    // NPE if s is null - not nullable.
                                    s.accept(Refed.create());
                                    return "ok";
                            }

                            @Override
                            public String a(Consumer<String> s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    s.accept("foo");
                                    return "ok";
                            }

                            @Override
                            public String b(Consumer<String> s) {
                                    // NPE if s is null - not nullable.
                                    s.accept("foo");
                                    return "ok";
                            }
                        };
                    }
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        var instance = createInstance("org.acme.mutiny.Foo", env);
        Consumer handler = x -> {
        };
        assertThat(instance).isNotNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Consumer.class, null))).isNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Consumer.class, handler))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "bar", Tuple2.of(Consumer.class, handler))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "bar", Tuple2.of(Consumer.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        assertThat((String) env.invoke(instance, "a", Tuple2.of(Consumer.class, null))).isNull();
        assertThat((String) env.invoke(instance, "a", Tuple2.of(Consumer.class, handler))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "b", Tuple2.of(Consumer.class, handler))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "b", Tuple2.of(Consumer.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void testNullableSupplierParameter() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Refed", """
                package org.acme;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Refed {
                    String m();

                    static Refed create() {
                        return new Refed() {
                            public String m() {
                                return "ok";
                            }
                        };
                    }
                }
                """);
        env.addJavaCode("org.acme", "Foo", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;
                import java.util.function.Supplier;

                @VertxGen
                public interface Foo {
                    @Nullable String foo(@Nullable Supplier<Refed> h);
                    String bar(Supplier<Refed> h);

                    @Nullable String a(@Nullable Supplier<String> h);
                    String b(Supplier<String> h);

                    static Foo create() {
                        return new Foo() {
                            @Override
                            public String foo(Supplier<Refed> s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    return s.get().m();
                            }

                            @Override
                            public String bar(Supplier<Refed> s) {
                                    // NPE if s is null - not nullable.
                                    return s.get().m();
                            }

                            @Override
                            public String a(Supplier<String> s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    return s.get();
                            }

                            @Override
                            public String b(Supplier<String> s) {
                                    // NPE if s is null - not nullable.
                                    return s.get();
                            }
                        };
                    }
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        var instance = createInstance("org.acme.mutiny.Foo", env);
        Supplier function = () -> createInstance("org.acme.mutiny.Refed", env);
        assertThat(instance).isNotNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Supplier.class, null))).isNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Supplier.class, function))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "bar", Tuple2.of(Supplier.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "bar", Tuple2.of(Supplier.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        function = () -> "ok";
        assertThat((String) env.invoke(instance, "a", Tuple2.of(Supplier.class, null))).isNull();
        assertThat((String) env.invoke(instance, "a", Tuple2.of(Supplier.class, function))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "b", Tuple2.of(Supplier.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "b", Tuple2.of(Supplier.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void testNullableListAndAndMapParameters() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Refed", """
                package org.acme;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Refed {
                    String m();

                    static Refed create() {
                        return new Refed() {
                            public String m() {
                                return "ok";
                            }
                        };
                    }
                }
                """);
        env.addJavaCode("org.acme", "Foo", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;
                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                @VertxGen
                public interface Foo {
                    @Nullable String listA(@Nullable List<Refed> h);
                    String listB(List<Refed> h);
                    @Nullable String listC(@Nullable List<String> h);
                    String listD(List<String> h);

                    @Nullable String setA(@Nullable Set<Refed> h);
                    String setB(Set<Refed> h);

                    @Nullable String setC(@Nullable Set<String> h);
                    String setD(Set<String> h);

                    @Nullable String mapA(@Nullable Map<String, Refed> h);
                    String mapB(Map<String, Refed> h);
                    @Nullable String mapC(@Nullable Map<String, String> h);
                    String mapD(Map<String, String> h);

                    static Foo create() {
                        return new Foo() {
                            public String listA(@Nullable List<Refed> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.get(0).m();
                            }
                            public String listB(List<Refed> h) {
                                return h.get(0).m();
                            }
                            public String listC(@Nullable List<String> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.get(0);
                            }
                            public String listD(List<String> h) {
                                return h.get(0);
                            }
                            public String setA(@Nullable Set<Refed> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.iterator().next().m();
                            }
                            public String setB(Set<Refed> h) {
                                return h.iterator().next().m();
                            }
                            public String setC(@Nullable Set<String> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.iterator().next();
                            }
                            public String setD(Set<String> h) {
                                return h.iterator().next();
                            }
                            public String mapA(@Nullable Map<String, Refed> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.get("foo").m();
                            }
                            public String mapB(Map<String, Refed> h) {
                                return h.get("foo").m();
                            }
                            public String mapC(@Nullable Map<String, String> h) {
                                if (h == null) {
                                    return null;
                                }
                                return h.get("foo");
                            }
                            public String mapD(Map<String, String> h) {
                                return h.get("foo");
                            }
                        };
                    }
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        var instance = createInstance("org.acme.mutiny.Foo", env);
        assertThat(instance).isNotNull();

        // @Nullable String listA(@Nullable List<Refed> h);
        // String listB(List<Refed> h);
        // @Nullable String listC(@Nullable List<String> h);
        // String listD(List<String> h);
        List list = List.of(createInstance("org.acme.mutiny.Refed", env));
        assertThat((String) env.invoke(instance, "listA", Tuple2.of(List.class, null))).isNull();
        assertThat((String) env.invoke(instance, "listA", Tuple2.of(List.class, list))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "listB", Tuple2.of(List.class, list))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "listB", Tuple2.of(List.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
        list = List.of("foo");
        assertThat((String) env.invoke(instance, "listC", Tuple2.of(List.class, null))).isNull();
        assertThat((String) env.invoke(instance, "listC", Tuple2.of(List.class, list))).isEqualTo("foo");
        assertThat((String) env.invoke(instance, "listD", Tuple2.of(List.class, list))).isEqualTo("foo");
        assertThatThrownBy(() -> env.invoke(instance, "listD", Tuple2.of(List.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        // @Nullable String setA(@Nullable Set<Refed> h);
        // String setB(Set<Refed> h);
        // @Nullable String setC(@Nullable Set<String> h);
        // String setD(List<String> h);
        Set set = Set.of(createInstance("org.acme.mutiny.Refed", env));
        assertThat((String) env.invoke(instance, "setA", Tuple2.of(Set.class, null))).isNull();
        assertThat((String) env.invoke(instance, "setA", Tuple2.of(Set.class, set))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "setB", Tuple2.of(Set.class, set))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "setB", Tuple2.of(Set.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
        set = Set.of("foo");
        assertThat((String) env.invoke(instance, "setC", Tuple2.of(Set.class, null))).isNull();
        assertThat((String) env.invoke(instance, "setC", Tuple2.of(Set.class, set))).isEqualTo("foo");
        assertThat((String) env.invoke(instance, "setD", Tuple2.of(Set.class, set))).isEqualTo("foo");
        assertThatThrownBy(() -> env.invoke(instance, "setD", Tuple2.of(Set.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        // @Nullable String mapA(@Nullable Map<String, Refed> h);
        // String mapB(Map<String, Refed> h);
        // @Nullable String mapC(@Nullable Map<String, String> h);
        // String mapD(Map<String, String> h);
        Map map = Map.of("foo", createInstance("org.acme.mutiny.Refed", env));
        assertThat((String) env.invoke(instance, "mapA", Tuple2.of(Map.class, null))).isNull();
        assertThat((String) env.invoke(instance, "mapA", Tuple2.of(Map.class, map))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "mapB", Tuple2.of(Map.class, map))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "mapB", Tuple2.of(Map.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
        map = Map.of("foo", "foo");
        assertThat((String) env.invoke(instance, "mapC", Tuple2.of(Map.class, null))).isNull();
        assertThat((String) env.invoke(instance, "mapC", Tuple2.of(Map.class, map))).isEqualTo("foo");
        assertThat((String) env.invoke(instance, "mapD", Tuple2.of(Map.class, map))).isEqualTo("foo");
        assertThatThrownBy(() -> env.invoke(instance, "mapD", Tuple2.of(Map.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void testNullableSupplierOfFutureParameter() {
        Env env = new Env();
        env.addModuleGen("org.acme", "org.acme", "foo");
        env.addJavaCode("org.acme", "Refed", """
                package org.acme;
                import io.vertx.codegen.annotations.VertxGen;

                @VertxGen
                public interface Refed {
                    String m();

                    static Refed create() {
                        return new Refed() {
                            public String m() {
                                return "ok";
                            }
                        };
                    }
                }
                """);
        env.addJavaCode("org.acme", "Foo", """
                package org.acme;

                import io.vertx.codegen.annotations.VertxGen;
                import io.vertx.codegen.annotations.Nullable;
                import java.util.function.Supplier;
                import io.vertx.core.Future;

                @VertxGen
                public interface Foo {
                    @Nullable String foo(@Nullable Supplier<Future<Refed>> h);
                    String bar(Supplier<Future<Refed>> h);

                    @Nullable String a(@Nullable Supplier<Future<String>> h);
                    String b(Supplier<Future<String>> h);

                    static Foo create() {
                        return new Foo() {
                            @Override
                            public String foo(Supplier<Future<Refed>> s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    return s.get().result().m();
                            }

                            @Override
                            public String bar(Supplier<Future<Refed>> s) {
                                    // NPE if s is null - not nullable.
                                    return s.get().result().m();
                            }

                            @Override
                            public String a(Supplier<Future<String>> s) {
                                    if (s == null) {
                                        return null;
                                    }
                                    return s.get().result();
                            }

                            @Override
                            public String b(Supplier<Future<String>> s) {
                                    // NPE if s is null - not nullable.
                                    return s.get().result();
                            }
                        };
                    }
                }
                """);

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        var instance = createInstance("org.acme.mutiny.Foo", env);
        Supplier function = () -> Uni.createFrom().item(createInstance("org.acme.mutiny.Refed", env));
        assertThat(instance).isNotNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Supplier.class, null))).isNull();
        assertThat((String) env.invoke(instance, "foo", Tuple2.of(Supplier.class, function))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "bar", Tuple2.of(Supplier.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "bar", Tuple2.of(Supplier.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);

        function = () -> Uni.createFrom().item("ok");
        assertThat((String) env.invoke(instance, "a", Tuple2.of(Supplier.class, null))).isNull();
        assertThat((String) env.invoke(instance, "a", Tuple2.of(Supplier.class, function))).isEqualTo("ok");
        assertThat((String) env.invoke(instance, "b", Tuple2.of(Supplier.class, function))).isEqualTo("ok");
        assertThatThrownBy(() -> env.invoke(instance, "b", Tuple2.of(Supplier.class, null)))
                .hasRootCauseInstanceOf(NullPointerException.class);
    }

    private Object createInstance(String clz, Env env) {
        return env.invoke(env.getClass(clz), "create");
    }

}
