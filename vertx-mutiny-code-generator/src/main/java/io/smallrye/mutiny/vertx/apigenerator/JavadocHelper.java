package io.smallrye.mutiny.vertx.apigenerator;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocDescriptionElement;
import com.github.javaparser.javadoc.description.JavadocInlineTag;

import io.smallrye.mutiny.vertx.apigenerator.analysis.ShimClass;
import io.smallrye.mutiny.vertx.apigenerator.collection.VertxGenClass;

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

    public static Javadoc removeReturnTag(Javadoc javadoc) {
        // Remove @return tag if any.
        List<JavadocBlockTag> tags = javadoc.getBlockTags();
        List<JavadocBlockTag> found = new ArrayList<>(); // Just in case a method has multiple return tags.
        for (JavadocBlockTag tag : tags) {
            if (tag.getTagName().equals("return")) {
                found.add(tag);
            }
        }
        tags.removeAll(found);
        return javadoc;
    }

    public static Javadoc replace(Javadoc jd, String v1, String v2) {
        String newValue = jd.getDescription().toText().replace(v1, v2);
        var newJavadoc = new Javadoc(JavadocDescription.parseText(newValue));
        for (var tag : jd.getBlockTags()) {
            newJavadoc.addBlockTag(tag);
        }
        return newJavadoc;
    }

    /**
     * Edits the given Javadoc to rewrite links to mention the Mutiny types instead of the Vert.x types.
     *
     * @param javadoc the javadoc to edit, can be {@code null}
     * @param shim the shim class, to access Vert.x Gen types
     * @return the transformed Javadoc
     */
    public static Javadoc toMutinyTypes(Javadoc javadoc, ShimClass shim) {
        if (javadoc == null) {
            return null;
        }
        List<JavadocDescriptionElement> elements = new ArrayList<>();
        for (JavadocDescriptionElement element : javadoc.getDescription().getElements()) {
            transformJavadocDescriptionElement(shim, element, elements);
        }

        Javadoc jd = new Javadoc(new JavadocDescription(elements));

        for (var tag : javadoc.getBlockTags()) {
            List<JavadocDescriptionElement> tagElements = new ArrayList<>();
            for (JavadocDescriptionElement element : tag.getContent().getElements()) {
                transformJavadocDescriptionElement(shim, element, tagElements);
            }
            JavadocDescription tagDescription = new JavadocDescription(tagElements);
            if (tag.getType() == JavadocBlockTag.Type.PARAM) {
                jd.addBlockTag(JavadocBlockTag.createParamBlockTag(tag.getName().orElse(""), tagDescription.toText()));
            } else {
                jd.addBlockTag(new JavadocBlockTag(tag.getType(), tagDescription.toText()));
            }
        }
        return jd;
    }

    private static void transformJavadocDescriptionElement(ShimClass shim, JavadocDescriptionElement element,
            List<JavadocDescriptionElement> elements) {
        if (element instanceof JavadocInlineTag) {
            JavadocInlineTag tag = (JavadocInlineTag) element;
            String link = tag.getContent();
            for (VertxGenClass clz : shim.getSource().getGenerator().getCollectionResult().allVertxGenClasses()) {
                if (link.contains(clz.fullyQualifiedName())) {
                    link = link.replace(clz.fullyQualifiedName(), clz.getShimClassName());
                }
            }
            elements.add(new JavadocInlineTag(tag.getName(), tag.getType(), link));
        } else {
            elements.add(element);
        }
    }

    public static Javadoc amendJavadocIfReturnTypeIsNullable(Javadoc javadoc) {
        if (javadoc == null) {
            return null;
        }
        JavadocBlockTag tag = javadoc.getBlockTags().stream()
                .filter(t -> t.getType() == JavadocBlockTag.Type.RETURN)
                .findFirst().orElse(null);
        if (tag == null) {
            return javadoc;
        }
        String text = tag.getContent().toText();
        if (text.contains("null")) {
            return javadoc; // Already mentioned
        }
        if (!text.endsWith(".")) {
            text += ".";
        }
        JavadocBlockTag newReturnTag = new JavadocBlockTag("return", text + " Can be {@code null}.");
        javadoc.getBlockTags().remove(tag);
        javadoc.addBlockTag(newReturnTag);
        return javadoc;
    }
}
