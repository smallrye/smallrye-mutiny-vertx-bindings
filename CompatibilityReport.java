///usr/bin/env jbang "$0" "$@" ; exit $? // (1)
//DEPS io.vertx:vertx-core:3.9.2
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.json.*;

class CompatibilityReport {

    public static List<Predicate<Difference>> IGNORED_ERROR_MESSAGES = List.of(
      d -> d.getDescription().equals("The return type changed from 'java.lang.Void' to  'void'."), // Yes, double space.
      d -> d.getDescription().equals("The type of the parameter changed from 'io.vertx.mutiny.core.buffer.Buffer' to 'io.vertx.core.buffer.Buffer'."),
      d -> isNewDataObjectDifference(d),
      d -> d.getDescription().contains("The return type changed from 'void' to ")  && d.getOldCode().contains("AndForget"),
      d -> d.getOldCode() != null  && d.getOldCode().contains("executeBlocking"),
      d -> d.getDescription().contains("io.vertx.mutiny.core.buffer.Buffer")  && d.getDescription().contains("io.vertx.core.buffer.Buffer")

    );

    public static boolean isNewDataObjectDifference(Difference diff) {
        var matcher = Pattern.compile("The return type changed from '(.*?)' to '(.*?)'\\.").matcher(diff.getDescription());
        if (!matcher.find()) {
            matcher = Pattern.compile("The type parameter of method parameter changed\\. The original type was '(.*?)'while the new type is '(.*?)'\\.").matcher(diff.getDescription());
        }

         if (matcher.find()) {
            var g1 = matcher.group(1).replace("io.vertx.mutiny.", "io.vertx.");
            var g2 = matcher.group(2).replace("io.vertx.mutiny.", "io.vertx.");
            return g1.equals(g2);
        } else {
            return false;
        }

    }

    public static void main(String[] args) throws Exception {
        System.out.println("Looking for compatibility reports");
        File current = new File(System.getProperty("user.dir"));
        File[] children = current.listFiles();
        Map<String, JsonArray> reports = new LinkedHashMap<>();
        for (File child : children) {
            if (child.isDirectory()) {
                File report = new File(child, "target/compatibility-report.json");
                if (report.isFile()) {
                    System.out.println("Found compatibility report " + report.getAbsolutePath());
                    JsonArray content = new JsonArray(Files.readString(report.toPath()));
                    reports.put(child.getName(), content);
                } else if (child.isDirectory()) {
                    for (File child2 : child.listFiles()) {
                        if (child2.isDirectory()) {
                            report = new File(child2, "target/compatibility-report.json");
                            if (report.isFile()) {
                                System.out.println("Found compatibility report " + report.getAbsolutePath());
                                JsonArray content = new JsonArray(Files.readString(report.toPath()));
                                reports.put(child2.getName(), content);
                            }
                        }
                    }
                }
            }
        }

        StringBuilder buffer = new StringBuilder();
        System.out.println(reports.size() + " reports found");
        buffer.append("# API Change analysis\n");
        ObjectMapper mapper = new ObjectMapper();

        for (Map.Entry<String, JsonArray> entry : reports.entrySet()) {
            JsonArray diff = entry.getValue();
            List<Difference> differences = mapper.readValue(diff.encode(), new TypeReference<List<Difference>>() {
            })
                    .stream()
                    .filter(f -> !f.isIgnored())
                    .filter(f -> !f.isCompatible())
                    .collect(Collectors.toList());

            if (differences.isEmpty()) {
                System.out.println("Skipping " + entry.getKey() + " - no changes or compatible changes");
                continue;
            }

            buffer.append("\n## Incompatible changes in ").append(entry.getKey()).append("\n");
            buffer.append("[cols=\"1,1,1,1\", options=\"header\"]\n");
            buffer.append("|===\n");
            buffer.append("| Element | Classification | Criticality | Description\n");

            buffer.append("\n");
            for (Difference difference : differences) {
                buffer.append("| `");
                buffer.append(difference.getOldCode());
                buffer.append("` a| ");
                buffer.append(difference.getCompatibility());
                buffer.append(" | " );
                buffer.append(difference.getCriticality());
                buffer.append(" | ");
                buffer.append(difference.getDescription());
                buffer.append("\n");
                buffer.append("\n");
            }
            buffer.append("|===\n");

            buffer.append("'''\n");
        }

        File target = new File("target");
        target.mkdirs();
        Files.write(new File(target, "compatibility-report.adoc").toPath(), buffer.toString().getBytes());
    }

    private static boolean isCompatible(List<Difference> diffs) {
        boolean isCompatible = true;
        for (Difference diff : diffs) {
            isCompatible = isCompatible && diff.isCompatible();
        }
        return isCompatible;
    }

    public static class Compatibility {
        private String compatibility;
        private String severity;

        public String getCompatibility() {
            return compatibility;
        }

        public Compatibility setCompatibility(String compatibility) {
            this.compatibility = compatibility;
            return this;
        }

        public String getSeverity() {
            return severity;
        }

        public Compatibility setSeverity(String severity) {
            this.severity = severity;
            return this;
        }
    }

    public static class Attachment {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public Attachment setName(String name) {
            this.name = name;
            return this;
        }

        public String getValue() {
            return value;
        }

        public Attachment setValue(String value) {
            this.value = value;
            return this;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Difference {
        private String code;
        @JsonProperty("old")
        private String oldCode;
        @JsonProperty("new")
        private String newCode;
        private String name;
        private String criticality;
        private String description;
        private Compatibility[] classification;
        private Attachment[] attachments;
        private String justification;

        public String getPackage() {
            return findAttachmentByName("package");
        }

        public String getClassName() {
            return findAttachmentByName("classQualifiedName");
        }

        public String getSimpleClassName() {
            return findAttachmentByName("classSimpleName");
        }

        public String getMethodName() {
            return findAttachmentByName("methodName");
        }

        public String getFieldName() {
            return findAttachmentByName("fieldName");
        }

        public String getNewArchive() {
            return findAttachmentByName("newArchive");
        }

        public String getCriticality() {
            return criticality;
        }

        public boolean isIgnored() {
            for (Predicate<Difference> predicate : IGNORED_ERROR_MESSAGES) {
                if (predicate.test(this)) {
                    return true;
                }
            }
            return false;
        }

        private String findAttachmentByName(String name) {
            if (attachments == null) {
                return null;
            } else {
                return Arrays.stream(attachments).filter(a -> a.name.equalsIgnoreCase(name))
                        .map(a -> a.value)
                        .findFirst()
                        .orElse(null);
            }
        }

        @Override
        public String toString() {
            return description;
        }

        private String findCompatibilityByName(String name) {
            if (classification == null) {
                return null;
            } else {
                return Arrays.stream(classification).filter(a -> a.compatibility.equalsIgnoreCase(name))
                        .map(a -> a.severity)
                        .findFirst()
                        .orElse(null);
            }
        }

        private static final List<String> COMPATIBLE = Arrays.asList("NON_BREAKING", "EQUIVALENT");

        public boolean isCompatible() {
            String source = findCompatibilityByName("SOURCE");
            String binary = findCompatibilityByName("BINARY");

            boolean isSourceCompatible = COMPATIBLE.contains(source.toUpperCase());
            boolean isBinaryCompatible = COMPATIBLE.contains(binary.toUpperCase());

            return isSourceCompatible  && isBinaryCompatible;
        }

        public String getElement() {
            String name = getMethodName();
            if (name == null) {
                name = getFieldName();
            }
            return getClassName() + "#" + name;
        }

        public String getCompatibility() {
            return "\n* Source: " + findCompatibilityByName("SOURCE") + " \n " + "* Binary: "
                    + findCompatibilityByName("BINARY");
        }

        public String getCode() {
            return code;
        }

        public Difference setCode(String code) {
            this.code = code;
            return this;
        }

        public String getOldCode() {
            return oldCode;
        }

        public Difference setOldCode(String oldCode) {
            this.oldCode = oldCode;
            return this;
        }

        public String getNewCode() {
            return newCode;
        }

        public Difference setNewCode(String newCode) {
            this.newCode = newCode;
            return this;
        }

        public String getName() {
            return name;
        }

        public Difference setName(String name) {
            this.name = name;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Difference setDescription(String description) {
            this.description = description;
            return this;
        }

        public Compatibility[] getClassification() {
            return classification;
        }

        public Difference setClassification(Compatibility[] classification) {
            this.classification = classification;
            return this;
        }

        public Attachment[] getAttachments() {
            return attachments;
        }

        public Difference setAttachments(Attachment[] attachments) {
            this.attachments = attachments;
            return this;
        }
    }
}
