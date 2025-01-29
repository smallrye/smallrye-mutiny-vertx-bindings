package io.smallrye.mutiny.vertx.apigenerator.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;

import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;

public class Env {

    public static final String DEFAULT_OUTPUT_DIRECTORY = "target/codegen-test";
    private final File root;
    private List<MutinyGenerator.GeneratorOutput> outputs = new ArrayList<>();

    public Env() {
        this(DEFAULT_OUTPUT_DIRECTORY);
    }

    public Env(File root) {
        this.root = new File(root, System.nanoTime() + "");
    }

    public Env(File root, String dir) {
        this.root = new File(root, dir);
    }

    public Env(String root) {
        this(new File(root));
    }

    public static MutinyGenerator.GeneratorOutput getOutputFor(List<MutinyGenerator.GeneratorOutput> outputs, String fqn) {
        for (MutinyGenerator.GeneratorOutput output : outputs) {
            if (output.genInterface().getFullyQualifiedName().equals(fqn)) {
                return output;
            }
        }
        throw new IllegalArgumentException("No output found for " + fqn);
    }

    public MutinyGenerator.GeneratorOutput getOutputFor(String fqn) {
        for (MutinyGenerator.GeneratorOutput output : outputs) {
            if (output.genInterface().getFullyQualifiedName().equals(fqn)) {
                return output;
            }
        }
        throw new IllegalArgumentException("No output found for " + fqn);
    }

    public static FieldSpec findField(MutinyGenerator.GeneratorOutput output, String fieldName) {
        return output.javaFile().typeSpec().fieldSpecs().stream()
                .filter(f -> f.name().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No field found for " + fieldName));
    }

    public static MethodSpec findMethod(MutinyGenerator.GeneratorOutput output, String methodName) {
        return output.javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No method found for " + methodName));
    }

    public static MethodSpec findMethod(MutinyGenerator.GeneratorOutput output, String methodName, String... params) {
        return output.javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().equals(methodName))
                .filter(m -> m.parameters().size() == params.length)
                .filter(m -> {
                    for (int i = 0; i < params.length; i++) {
                        if (!m.parameters().get(i).type().toString().equals(params[i])) {
                            return false;
                        }
                    }
                    return true;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No method found for " + methodName));
    }

    public static MethodSpec findConstructor(MutinyGenerator.GeneratorOutput output, String... params) {
        return output.javaFile().typeSpec().methodSpecs().stream()
                .filter(m -> m.name().equals("<init>"))
                .filter(m -> m.parameters().size() == params.length)
                .filter(m -> {
                    for (int i = 0; i < params.length; i++) {
                        if (!m.parameters().get(i).type().toString().equals(params[i])) {
                            return false;
                        }
                    }
                    return true;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No constructor found for " + Arrays.toString(params)));
    }

    public Env addJavaCode(String packageName, String fileName, String code) {
        InMemorySource source = new InMemorySource(packageName + "." + fileName.replace(".java", ""), code);
        sources.add(source);
        source.write(root);
        return this;
    }

    public Path root() {
        return root.toPath();
    }

    public Env addModuleGen(String packageName, String name) {
        String p = packageName.replace(".", "/");
        File packageDir = new File(root, p);
        packageDir.mkdirs();

        File pi = new File(packageDir, "package-info.java");
        String content = """

                @ModuleGen(name = "%s", groupPackage = "%s")
                package %s;

                import io.vertx.codegen.annotations.ModuleGen;

                """.formatted(name, packageName, packageName);

        try {
            Files.write(pi.toPath(), content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Unable to write package-info file on disk", e);
        }
        return this;
    }

    public Env addModuleGen(String packageName, String groupName, String name) {
        String p = packageName.replace(".", "/");
        File packageDir = new File(root, p);
        packageDir.mkdirs();

        File pi = new File(packageDir, "package-info.java");
        String content = """

                @ModuleGen(name = "%s", groupPackage = "%s")
                package %s;

                import io.vertx.codegen.annotations.ModuleGen;

                """.formatted(name, groupName, packageName);

        try {
            Files.write(pi.toPath(), content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Unable to write package-info file on disk", e);
        }
        return this;
    }

    private final List<InMemorySource> sources = new ArrayList<>();

    public Env addOutputs(List<MutinyGenerator.GeneratorOutput> output) {
        this.outputs.addAll(output);
        for (MutinyGenerator.GeneratorOutput o : output) {
            InMemorySource source = new InMemorySource(o.shim().getFullyQualifiedName(), o.javaFile().toString());
            sources.add(source);
        }
        return this;
    }

    public List<Class<?>> compile() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "No system Java compiler found. Are you running a JRE instead of a JDK?");
        try (var fileMgr = compiler.getStandardFileManager(null, null, null)) {
            var inMemoryFileManager = new InMemoryFileManager(fileMgr);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    inMemoryFileManager,
                    null,
                    null,
                    null,
                    sources);

            Boolean result = task.call();
            assertTrue(result, "Compilation failed.");
            Map<String, byte[]> compiledClasses = inMemoryFileManager.getCompiledClasses();
            InMemoryClassLoader inMemoryClassLoader = new InMemoryClassLoader(compiledClasses,
                    this.getClass().getClassLoader());

            for (var className : compiledClasses.keySet()) {
                Class<?> clz = inMemoryClassLoader.loadClass(className);
                classes.add(clz);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return classes;
    }

    private List<Class<?>> classes = new ArrayList<>();

    public void dump(String path) {
        for (InMemorySource source : sources) {
            source.write(new File(path));
        }
    }

    public void dumpGeneratedCodeTo(PrintStream printStream) {
        for (InMemorySource source : sources) {
            printStream.writeBytes(("\n=== " + source.getName() + " ===\n").getBytes(StandardCharsets.UTF_8));
            printStream.writeBytes(source.code.getBytes(StandardCharsets.UTF_8));
        }
    }

    public Env addPackage(String sourceRoot, String packageName) {
        File packageDir = new File(sourceRoot, packageName.replace(".", "/"));
        File[] files = packageDir.listFiles();
        if (files == null) {
            return this;
        }
        for (File file : files) {
            if (file.getName().endsWith(".java")) {
                try {
                    String code = Files.readString(file.toPath());
                    addJavaCode(packageName, file.getName(), code);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read file " + file, e);
                }
            }
        }
        return this;
    }

    public Class<?> getClass(String classname) {
        for (Class<?> c : classes) {
            if (c.getName().equals(classname)) {
                return c;
            }
        }
        throw new NoSuchElementException("No class found for " + classname);
    }

    @SuppressWarnings("unchecked")
    public <T> T invoke(Class<T> clazz, String methodName, Tuple2<Class<?>, Object>... args) {
        try {
            Class<?>[] types = Stream.of(args).map(Tuple2::getItem1).toArray(Class[]::new);
            Object[] values = Stream.of(args).map(Tuple2::getItem2).toArray();
            var method = clazz.getDeclaredMethod(methodName, types);
            return (T) method.invoke(null, values);
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke method " + methodName + " on " + clazz, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T invoke(Object object, String methodName, Tuple2<Class<?>, Object>... args) {
        try {
            Class<?>[] types = Stream.of(args).map(Tuple2::getItem1).toArray(Class[]::new);
            Object[] values = Stream.of(args).map(Tuple2::getItem2).toArray();
            var method = object.getClass().getDeclaredMethod(methodName, types);
            method.setAccessible(true);
            return (T) method.invoke(object, values);
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke method `" + methodName + "` on " + object, e);
        }
    }

    public static class InMemorySource extends SimpleJavaFileObject {
        private final String code;
        private final String packageName;
        private final String className;

        public InMemorySource(String className, String sourceCode) {
            super(URI.create("string:///"
                    + className.replace('.', '/')
                    + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.packageName = className.substring(0, className.lastIndexOf('.'));
            this.className = className;
            this.code = sourceCode;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }

        public void write(File root) {
            String p = packageName.replace(".", "/");
            File packageDir = new File(root, p);
            packageDir.mkdirs();
            File file = new File(root, p + "/" + className.substring(className.lastIndexOf('.') + 1) + ".java");
            try {
                Files.write(file.toPath(), code.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Unable to write class file on disk", e);
            }
        }
    }

    public static class InMemoryClassFile extends SimpleJavaFileObject {
        private final ByteArrayOutputStream byteCode = new ByteArrayOutputStream();

        public InMemoryClassFile(String className, Kind kind) {
            super(URI.create("mem:///"
                    + className.replace('.', '/')
                    + kind.extension),
                    kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return byteCode;
        }

        public byte[] getByteCode() {
            return byteCode.toByteArray();
        }
    }

    public static class InMemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> compiledClasses;

        public InMemoryClassLoader(Map<String, byte[]> compiledClasses, ClassLoader parent) {
            super(parent);
            this.compiledClasses = compiledClasses;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (compiledClasses.containsKey(name)) {
                byte[] bytes = compiledClasses.get(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }

    }

    public static class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {

        // Map of className -> compiled bytecode
        private final Map<String, InMemoryClassFile> compiledClasses = new HashMap<>();

        protected InMemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                Location location,
                String className,
                JavaFileObject.Kind kind,
                FileObject sibling) {
            InMemoryClassFile inMemoryFile = new InMemoryClassFile(className, kind);
            compiledClasses.put(className, inMemoryFile);
            return inMemoryFile;
        }

        public Map<String, byte[]> getCompiledClasses() {
            // Extract the bytes from each InMemoryClassFile
            Map<String, byte[]> result = new HashMap<>();
            for (var e : compiledClasses.entrySet()) {
                result.put(e.getKey(), e.getValue().getByteCode());
            }
            return result;
        }
    }

}
