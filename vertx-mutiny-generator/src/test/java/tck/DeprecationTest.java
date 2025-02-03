package tck;

import io.vertx.mutiny.codegen.testmodel.DeprecatedType;
import org.junit.Test;

import static org.junit.Assert.*;

public class DeprecationTest {

    @Test
    public void checkThatAnnotationsAreForwarded() throws NoSuchMethodException {
        Deprecated[] annotationsByType = DeprecatedType.class.getAnnotationsByType(Deprecated.class);
        assertEquals(1, annotationsByType.length);

        Deprecated deprecated = DeprecatedType.class.getMethod("someMethod").getAnnotation(Deprecated.class);
        assertNull(deprecated);

        deprecated = DeprecatedType.class.getMethod("someOtherMethod").getAnnotation(Deprecated.class);
        assertNotNull(deprecated);
    }
}
