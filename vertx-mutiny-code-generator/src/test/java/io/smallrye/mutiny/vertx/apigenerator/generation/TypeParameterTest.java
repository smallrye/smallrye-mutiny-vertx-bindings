package io.smallrye.mutiny.vertx.apigenerator.generation;

import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.smallrye.mutiny.vertx.apigenerator.tests.Env;
import org.junit.jupiter.api.Test;

public class TypeParameterTest {


    @Test
    void testRowSet() {
        Env env = new Env()
                .addJavaCode("io.vertx.sqlclient", "RowSet.java", """
                        package io.vertx.sqlclient;
                        
                        import io.vertx.codegen.annotations.VertxGen;
                        
                        @VertxGen
                        public interface RowSet<R> extends Iterable<R>, SqlResult<RowSet<R>> {
                        
                          @Override
                          RowIterator<R> iterator();
                        
                          @Override
                          RowSet<R> next();
                        
                        }
                        """)
                .addJavaCode("io.vertx.sqlclient", "RowIterator.java", """
                        package io.vertx.sqlclient;
                        
                        import io.vertx.codegen.annotations.VertxGen;
                        
                        import java.util.Iterator;
                        
                        /**
                         * An iterator for processing rows.
                         */
                        @VertxGen
                        public interface RowIterator<R> extends Iterator<R> {
                        
                          @Override
                          boolean hasNext();
                        
                          @Override
                          R next();
                        
                        }
                        """)
                .addJavaCode("io.vertx.sqlclient", "SqlResult.java", """
                        package io.vertx.sqlclient;
                        
                        import io.vertx.codegen.annotations.VertxGen;
                        import java.util.List;
                        @VertxGen
                        public interface SqlResult<T> {
                        
                          /**
                           * Get the number of the affected rows in the operation to this SqlResult.
                           *
                           * @return the count of affected rows.
                           */
                          int rowCount();
                        
                          /**
                           * Get the names of columns in the SqlResult.
                           *
                           * @return the list of names of columns.
                           */
                          List<String> columnsNames();
                        
                          /**
                           * Get the number of rows retrieved in the SqlResult.
                           *
                           * @return the count of rows.
                           */
                          int size();
                        
                          /**
                           * Get the specific property with the specified {@link PropertyKind}.
                           *
                           * @param propertyKind the unique object which is used to indicate which property of the execution result to fetch
                           * @param <V> the type of the property value
                           * @return the value of the property
                           */
                          <V> V property(PropertyKind<V> propertyKind);
                        
                          /**
                           * Get the execution result value, the execution result type may vary such as a {@link RowSet rowSet} or even a {@link String string}.
                           *
                           * @return the result value
                           */
                          T value();
                        
                          /**
                           * Return the next available result or {@code null}, e.g for a simple query that executed multiple queries or for
                           * a batch result.
                           *
                           * @return the next available result or {@code null} if none is available
                           */
                          SqlResult<T> next();
                        
                        }
                        """)
                .addJavaCode("io.vertx.sqlclient", "PropertyKind.java", """
                        package io.vertx.sqlclient;
                        
                        import io.vertx.codegen.annotations.GenIgnore;
                        import io.vertx.codegen.annotations.VertxGen;
                        
                        import java.util.Objects;
                        
                        /**
                         * The kind of the property, this can be used to fetch some specific property of the {@link SqlResult execution result}.
                         */
                        @VertxGen
                        public interface PropertyKind<T> {
                        
                          /**
                           * @return a property kind matching the provided {@code name}, the {@code type} can be used to check
                           *         the property value type or cast it to the expected type
                           */
                          static <T> PropertyKind<T> create(String name, Class<T> type) {
                            Objects.requireNonNull(name, "No null name accepted");
                            Objects.requireNonNull(type, "No null type accepted");
                            return new PropertyKind<T>() {
                              @Override
                              public String name() {
                                return name;
                              }
                              @Override
                              public Class<T> type() {
                                return type;
                              }
                              @Override
                              public int hashCode() {
                                return name.hashCode();
                              }
                              @Override
                              public boolean equals(Object obj) {
                                if (obj == this) {
                                  return true;
                                } else if (obj instanceof PropertyKind) {
                                  return name.equals(((PropertyKind)obj).name());
                                } else {
                                  return false;
                                }
                              }
                              @Override
                              public String toString() {
                                return "PropertyKind[name=" + name + ",type=" + type.getName();
                              }
                            };
                          }
                        
                          /**
                           * @return the property name
                           */
                          String name();
                        
                          /**
                           * @return the property type
                           */
                          @GenIgnore
                          Class<T> type();
                        }
                        """)
                .addModuleGen("io.vertx", "vertx-sql-client");

        MutinyGenerator generator = new MutinyGenerator(env.root());
        env.addOutputs(generator.generate());

        System.out.println(env.getOutputFor("io.vertx.sqlclient.RowSet").javaFile());

        env.compile();

    }

}
