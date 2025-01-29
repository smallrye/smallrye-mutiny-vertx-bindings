package io.smallrye.mutiny.vertx.apigenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import io.smallrye.mutiny.vertx.apigenerator.types.TypeDescriber;

public class ConsumerTest {

    @Test
    void testInterfaceExtendingHandlerOfDataObject() {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "MyDataObject.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.DataObject;

                        @DataObject
                        public class MyDataObject {
                            public MyDataObject() {
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;
                        import io.vertx.core.json.JsonObject;

                        @VertxGen
                        public interface MyInterface extends Handler<MyDataObject> {
                            void handle(MyDataObject data);
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyInterface");

        assertThat(output.shim().getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.core.Handler<org.acme.MyDataObject>")
                .contains(Consumer.class.getName() + "<org.acme.MyDataObject>");

        env.compile();
    }

    @Test
    void testInterfaceExtendingHandlerOfPlainObject() {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;
                        import io.vertx.core.json.JsonObject;

                        @VertxGen
                        public interface MyInterface extends Handler<String> {
                            void handle(String data);
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyInterface");

        assertThat(output.shim().getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.core.Handler<java.lang.String>")
                .contains(Consumer.class.getName() + "<java.lang.String>");

        env.compile();
    }

    @Test
    void testInterfaceExtendingHandlerOfVertxGen() {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "MyVG.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyVG {
                            String foo();
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;

                        @VertxGen
                        public interface MyInterface extends Handler<MyVG> {
                            void handle(MyVG data);
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyInterface");

        assertThat(output.shim().getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.core.Handler<org.acme.mutiny.MyVG>")
                .contains(Consumer.class.getName() + "<org.acme.mutiny.MyVG>");

        env.compile();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testClassAcceptingHandlerOfDataObject() throws Exception {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "MyDataObject.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.DataObject;

                        @DataObject
                        public class MyDataObject {
                            public MyDataObject() {
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;
                        import io.vertx.core.json.JsonObject;

                        @VertxGen
                        public interface MyInterface {
                            void handler(Handler<MyDataObject> handler);
                            void emit(MyDataObject data);

                            public static MyInterface create() {
                                return new MyInterface() {
                                    Handler<MyDataObject> handler;
                                    @Override
                                    public void handler(Handler<MyDataObject> handler) {
                                        this.handler = handler;
                                    }

                                    @Override
                                    public void emit(MyDataObject data) {
                                        if (this.handler != null) {
                                            this.handler.handle(data);
                                        }
                                    }
                                };
                            }
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        Object instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat(instance.getClass().getName()).isEqualTo("org.acme.mutiny.MyInterface");

        AtomicReference<Object> reference = new AtomicReference<>();

        env.invoke(instance, "handler", Tuple2.of(Consumer.class, new Consumer<Object>() {
            @Override
            public void accept(Object o) {
                reference.set(o);
            }
        }));

        Object r = env.getClass("org.acme.MyDataObject").getDeclaredConstructor().newInstance();
        env.invoke(instance, "emit", Tuple2.of(r.getClass(), r));
        assertThat(reference.get()).isNotNull().isEqualTo(r);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testClassAcceptingHandlerOfPlainObject() throws Exception {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;
                        import io.vertx.core.json.JsonObject;

                        @VertxGen
                        public interface MyInterface {
                            void handler(Handler<String> handler);
                            void emit(String data);

                            public static MyInterface create() {
                                return new MyInterface() {
                                    Handler<String> handler;
                                    @Override
                                    public void handler(Handler<String> handler) {
                                        this.handler = handler;
                                    }

                                    @Override
                                    public void emit(String data) {
                                        if (this.handler != null) {
                                            this.handler.handle(data);
                                        }
                                    }
                                };
                            }
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        Object instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat(instance.getClass().getName()).isEqualTo("org.acme.mutiny.MyInterface");

        AtomicReference<String> reference = new AtomicReference<>();

        env.invoke(instance, "handler", Tuple2.of(Consumer.class, new Consumer<String>() {
            @Override
            public void accept(String o) {
                reference.set(o);
            }
        }));

        env.invoke(instance, "emit", Tuple2.of(String.class, "hello"));
        assertThat(reference.get()).isNotNull().isEqualTo("hello");
    }

    @Test
    void testClassAcceptingHandlerOfTypeParameter() {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;
                        import io.vertx.core.json.JsonObject;

                        @VertxGen
                        public interface MyInterface<T> {
                            void handler(Handler<T> handler);
                            void emit(T data);

                            public static <T> MyInterface<T> create() {
                                return new MyInterface<>() {
                                    Handler<T> handler;
                                    @Override
                                    public void handler(Handler<T> handler) {
                                        this.handler = handler;
                                    }

                                    @Override
                                    public void emit(T data) {
                                        if (this.handler != null) {
                                            this.handler.handle(data);
                                        }
                                    }
                                };
                            }
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testClassAcceptingHandlerOfVertxGenObject() throws Exception {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "MyVG.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface MyVG {
                            String foo();

                            static MyVG create() {
                            return new MyVG() {
                                @Override
                                public String foo() {
                                    return "hello";
                                }
                            };
                            }
                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;
                        import io.vertx.core.json.JsonObject;

                        @VertxGen
                        public interface MyInterface {
                            void handler(Handler<MyVG> handler);
                            void emit(MyVG data);

                            public static MyInterface create() {
                                return new MyInterface() {
                                    Handler<MyVG> handler;
                                    @Override
                                    public void handler(Handler<MyVG> handler) {
                                        this.handler = handler;
                                    }

                                    @Override
                                    public void emit(MyVG data) {
                                        if (this.handler != null) {
                                            this.handler.handle(data);
                                        }
                                    }
                                };
                            }
                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        env.compile();
        Class<?> clz = env.getClass("org.acme.mutiny.MyInterface");
        Object instance = env.invoke(clz, "create");
        assertThat(instance).isNotNull();
        assertThat(instance.getClass().getName()).isEqualTo("org.acme.mutiny.MyInterface");

        AtomicReference<Object> reference = new AtomicReference<>();

        env.invoke(instance, "handler", Tuple2.of(Consumer.class, new Consumer<Object>() {
            @Override
            public void accept(Object o) {
                reference.set(o);
            }
        }));

        Object r = env.invoke(env.getClass("org.acme.mutiny.MyVG"), "create");
        env.invoke(instance, "emit", Tuple2.of(r.getClass(), r));
        assertThat(reference.get()).isNotNull().isEqualTo(r);
    }

    @Test
    void testInterfaceNotDefiningHandler() {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;
                        import io.vertx.core.json.JsonObject;

                        @VertxGen
                        public interface MyInterface extends Handler<String> {

                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyInterface");

        assertThat(output.shim().getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.core.Handler<java.lang.String>")
                .contains(Consumer.class.getName() + "<java.lang.String>");

        env.compile();
    }

    @Test
    void testInterfaceNotDefiningHandlerWhenConsumingVertxGen() {
        Env env = new Env();
        env
                .addJavaCode("org.acme", "VG.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;

                        @VertxGen
                        public interface VG  {

                        }
                        """)
                .addJavaCode("org.acme", "MyInterface.java", """
                        package org.acme;

                        import io.vertx.codegen.annotations.VertxGen;
                        import io.vertx.core.Handler;

                        @VertxGen
                        public interface MyInterface extends Handler<VG> {

                        }
                        """)
                .addModuleGen("org.acme", "my-module");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        MutinyGenerator.GeneratorOutput output = env.getOutputFor("org.acme.MyInterface");

        assertThat(output.shim().getInterfaces().stream().map(TypeDescriber::safeDescribeType).toList())
                .contains("io.vertx.core.Handler<org.acme.mutiny.VG>")
                .contains(Consumer.class.getName() + "<org.acme.mutiny.VG>");

        env.compile();
    }

}
