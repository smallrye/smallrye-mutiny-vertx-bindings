package io.smallrye.mutiny.vertx.apigenerator.collection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;

import java.util.List;
import java.util.NoSuchElementException;

public record CollectionResult(
        List<CompilationUnit> units,
        VertxGenModule module,
        List<VertxGenInterface> interfaces,
        List<VertxGenClass> allVertxGenClasses,
        List<String> allDataObjects,
        List<VertxGenModule> allModules,
        JavaSymbolSolver solver) {

    public boolean isVertxGen(String name) {
        return allVertxGenClasses.stream().anyMatch(c -> c.fullyQualifiedName().equals(name));
    }

    public VertxGenModule getModuleForVertxGen(String name) {
        return allVertxGenClasses.stream()
                .filter(c -> c.fullyQualifiedName().equals(name))
                .map(VertxGenClass::module)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find module for " + name));
    }

    public VertxGenClass getVertxGenClass(String name) {
        return allVertxGenClasses.stream()
                .filter(c -> c.fullyQualifiedName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find class " + name));
    }

    public boolean isDataObject(String className) {
        return allDataObjects.contains(className);
    }

    public VertxGenInterface getInterface(String className) {
        return interfaces.stream()
                .filter(i -> i.getFullyQualifiedName().equals(className))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot find interface " + className + " in "
                        + interfaces.stream().map(VertxGenInterface::getFullyQualifiedName).toList()));
    }

    public List<CompilationUnit> allCompilationUnits() {
        return units;
    }
}
