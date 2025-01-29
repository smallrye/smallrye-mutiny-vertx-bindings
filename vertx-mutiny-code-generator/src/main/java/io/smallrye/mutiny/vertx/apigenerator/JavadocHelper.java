package io.smallrye.mutiny.vertx.apigenerator;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;

public class JavadocHelper {

    public static Javadoc addToJavadoc(Javadoc javadoc, String content) {
        String doc;
        if (javadoc == null) {
            doc = """
                    %s
                    """.formatted(content);
            JavadocDescription description = JavadocDescription.parseText(doc);
            return new Javadoc(description);
        } else {
            doc = """
                    %s
                    %s
                    """.formatted(javadoc.getDescription().toText(), content);
            JavadocDescription description = JavadocDescription.parseText(doc);
            Javadoc copy = new Javadoc(description);
            for (var tag : javadoc.getBlockTags()) {
                copy.addBlockTag(tag);
            }
            return copy;
        }
    }

    public static Javadoc addOrReplaceReturnTag(Javadoc javadoc, String value) {
        if (javadoc == null) {
            return new Javadoc(JavadocDescription.parseText("""
                    @return %s
                    """.formatted(value)));
        }
        // Remove @return tag if any.
        List<JavadocBlockTag> tags = javadoc.getBlockTags();
        List<JavadocBlockTag> found = new ArrayList<>(); // Just in case a method has multiple return tags.
        for (JavadocBlockTag tag : tags) {
            if (tag.getTagName().equals("return")) {
                found.add(tag);
            }
        }
        tags.removeAll(found);
        return javadoc.addBlockTag("return", value);
    }

    public static Javadoc replace(Javadoc jd, String v1, String v2) {
        String newValue = jd.getDescription().toText().replace(v1, v2);
        var newJavadoc = new Javadoc(JavadocDescription.parseText(newValue));
        for (var tag : jd.getBlockTags()) {
            newJavadoc.addBlockTag(tag);
        }
        return newJavadoc;
    }
}
