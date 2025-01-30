package io.smallrye.mutiny.vertx.apigenerator.collection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;

import io.smallrye.mutiny.vertx.apigenerator.AnnotationHelper;
import io.smallrye.mutiny.vertx.apigenerator.MutinyGenerator;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.codegen.annotations.VertxGen;

public class VertxGenCollection {

    public static final String CONCRETE_ATTRIBUTE = "concrete";
    private static final Logger logger = LoggerFactory.getLogger(VertxGenCollection.class);
    private final JavaSymbolSolver solver;
    private final List<CompilationUnit> units;

    private final List<VertxGenModule> modules = new ArrayList<>();
    private final List<String> dataObjects = new ArrayList<>();
    private final List<VertxGenClass> allInterfaces = new ArrayList<>();
    private final MutinyGenerator generator;

    private final List<VertxGenInterface> interfaces = new ArrayList<>();
    private final List<CompilationUnit> additionalUnits;

    public VertxGenCollection(MutinyGenerator generator, SourceRoot source, List<Path> additionSources) throws IOException {
        this.generator = generator;
        logger.info("Initializing collection of sources from `{}`", source.getRoot());
        logger.info("Parsing source code from `{}`", source);
        List<TypeSolver> solvers = new ArrayList<>();
        solvers.add(new JavaParserTypeSolver(source.getRoot()));
        for (Path path : additionSources) {
            solvers.add(new JavaParserTypeSolver(path));
        }
        solvers.add(new ReflectionTypeSolver(false));
        TypeSolver typeSolver = new CombinedTypeSolver(solvers);
        solver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(solver);

        source.setParserConfiguration(source.getParserConfiguration().setSymbolResolver(solver));
        source.tryToParse();
        units = source.getCompilationUnits();

        List<CompilationUnit> additionalUnits = new ArrayList<>();
        for (Path path : additionSources) {
            SourceRoot root = new SourceRoot(path);
            root.setParserConfiguration(root.getParserConfiguration().setSymbolResolver(solver));
            root.tryToParse();
            additionalUnits.addAll(root.getCompilationUnits());
        }
        this.additionalUnits = additionalUnits;

        // Collect the Vert.x Gen modules
        logger.info("Collecting Vert.x Gen Modules");
        modules.addAll(collectVertxGenModuleFromCompilationUnits(units));
        logger.info("Found {} modules in the primary sources: {}", modules.size(),
                modules.stream().map(VertxGenModule::name).toList());
        modules.addAll(collectVertxGenModuleFromCompilationUnits(additionalUnits));
        logger.info("Found {} modules (primary and additional sources): {}", modules.size(),
                modules.stream().map(VertxGenModule::name).toList());

        merge(modules);

        // Collect the data objects
        logger.info("Collecting data objects");
        dataObjects.addAll(collectDataObjectsFromCompilationUnits(units));
        dataObjects.addAll(collectDataObjectsFromCompilationUnits(additionalUnits));
        logger.info("Found {} data objects (primary and additional sources): {}", dataObjects.size(), dataObjects);

        // Collect all Vert.x Gen interfaces
        logger.info("Collecting Vert.x Gen interfaces");
        allInterfaces.addAll(collectVertxGenFromCompilationUnits(units));
        logger.info("Found {} Vert.x Gen interfaces in the primary sources", allInterfaces.size());
        allInterfaces.addAll(collectVertxGenFromCompilationUnits(additionalUnits));
    }

    /**
     * Multi package-info can have the same module name, we need to merge them.
     * This means, multiple package names per module.
     * <p>
     * The merge is "in-place", so merged modules are removed.
     *
     * @param modules the list of modules
     */
    private void merge(List<VertxGenModule> modules) {
        // First organize the modules in a multimap: name -> modules
        Map<String, List<VertxGenModule>> map = new HashMap<>();
        for (VertxGenModule module : modules) {
            map.computeIfAbsent(module.name(), k -> new ArrayList<>()).add(module);
        }
        // For each list with more than one module - merge them.
        for (List<VertxGenModule> list : map.values()) {
            if (list.size() > 1) {
                VertxGenModule merged = list.getFirst(); // This will be the module in which we merge.
                for (int i = 1; i < list.size(); i++) {
                    merged = VertxGenModule.merge(merged, list.get(i));
                }
                modules.removeAll(list);
                modules.add(merged);
            }
        }
    }

    public CollectionResult collect(String moduleName) {
        VertxGenModule module = lookupModule(moduleName);
        logger.info("Processing module `{}` with group `{}", module.name(), module.group());
        logger.info("Collecting Vert.x Gen interfaces for module `{}`", module.name());

        interfaces.addAll(collectInterfacesForModule(units, module));
        logger.info("Found {} interfaces in module `{}`", interfaces.size(), module.name());

        logger.info("Collection completed");
        return new CollectionResult(
                units,
                module,
                interfaces,
                allInterfaces,
                dataObjects,
                modules,
                solver);
    }

    public List<VertxGenInterface> collectInterfacesForModule(List<CompilationUnit> compilations, VertxGenModule module) {
        logger.info("Collecting Vert.x Gen interfaces");
        List<VertxGenInterface> foundInterfaces = new ArrayList<>();
        for (CompilationUnit unit : compilations) {
            var list = unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(ClassOrInterfaceDeclaration::isInterface)
                    .filter(c -> c.isAnnotationPresent(VertxGen.class))
                    .toList();
            for (ClassOrInterfaceDeclaration vg : list) {
                String fqn = unit.getPackageDeclaration().map(p -> p.getName().asString() + ".").orElse("")
                        + vg.getNameAsString();
                if (!belongsToModule(fqn, module)) {
                    logger.warn("Ignoring `{}` as it does not belong to the module `{}`", fqn, module.name());
                    continue;
                }
                AnnotationExpr annotation = vg.getAnnotationByClass(VertxGen.class)
                        .orElseThrow(() -> new IllegalStateException("VertxGen annotation expected"));
                boolean concrete = AnnotationHelper.getAttribute(annotation, CONCRETE_ATTRIBUTE)
                        .map(p -> p.getValue().asBooleanLiteralExpr().getValue()).orElse(true);

                // We need to collect the methods in 2 phases:
                // 1) we only consider the method from the interface itself
                // 2) we consider overridden methods from the extended interfaces

                // Because of type parameters, we may need to update the method from interface to apply the right type parameter.
                // Also, because of overload, some method will need to be removed if it conflicts with a method already added.

                List<VertxGenMethod> methods = new ArrayList<>();
                ResolvedReferenceTypeDeclaration resolvedAnalyzedInterface = vg.resolve();

                // Compute the type parameter mapping
                Map<String, ResolvedType> typeParameterMapping = new LinkedHashMap<>();
                for (ResolvedReferenceType ancestor : resolvedAnalyzedInterface.getAllAncestors()) {
                    for (var pair : ancestor.getTypeParametersMap()) {
                        typeParameterMapping.put(ancestor.getQualifiedName() + "." + pair.a.getName(), pair.b);
                    }
                }

                // Phase 1)

                vg.getMethods().stream()
                        .filter(m -> !AnnotationHelper.isIgnored(m))
                        .filter(m -> (m.resolve().getPackageName() + "." + m.resolve().getClassName()).equals(fqn))
                        .filter(m -> !isMethodAlreadyInList(m.toMethodDeclaration().orElseThrow().resolve(), methods))
                        .map(VertxGenMethod::new).forEach(methods::add);

                // Phase 2)

                for (MethodUsage method : resolvedAnalyzedInterface.getAllMethods()) {
                    // getAllMethods returns all methods, including the one from the interface and extended types
                    // It removes overloads and methods that are overridden - except when there are type parameters

                    ResolvedMethodDeclaration resolvedDeclaration = method.getDeclaration();
                    ResolvedReferenceTypeDeclaration typeDeclaringMethod = resolvedDeclaration.declaringType();
                    MethodDeclaration astDeclaration = (MethodDeclaration) resolvedDeclaration.toAst().orElse(null);

                    // Ignore if the method is declared on the interface itself
                    if (typeDeclaringMethod.getQualifiedName().equals(resolvedAnalyzedInterface.getQualifiedName())) {
                        logger.debug("Ignoring method `{}` in interface `{}` as it is declared on the interface itself",
                                method.getName(), fqn);
                        continue;
                    }

                    // Ignore static methods from a parent interface
                    if (resolvedDeclaration.isStatic()) {
                        logger.debug(
                                "Ignoring method `{}` in interface `{}` as it is a static method from the parent interface",
                                method.getName(), fqn);
                        continue;
                    }

                    if (astDeclaration == null) {
                        astDeclaration = lookForMethodDeclaration(typeDeclaringMethod, method);
                        if (astDeclaration == null) {
                            logger.warn("Cannot resolve the AST declaration of {} of type {} declared in {}",
                                    method.getName(),
                                    resolvedDeclaration.toDescriptor(),
                                    typeDeclaringMethod.getQualifiedName());
                            continue;
                        }
                    }

                    // Ignore the method if it is annotated with @GenIgnore
                    if (AnnotationHelper.isIgnored(astDeclaration)) {
                        logger.debug("Ignoring method `{}` in interface `{}` as it is annotated with @GenIgnore",
                                method.getName(), fqn);
                        continue;
                    }

                    // Now, we only consider methods from extended interface.
                    // We should only consider methods from Vert.x Gen interfaces.
                    if (!typeDeclaringMethod.hasAnnotation(VertxGen.class.getName())) {
                        logger.debug("Ignoring method `{}` in interface `{}` as it is not annotated with @VertxGen",
                                method.getName(), fqn);
                        continue;
                    }

                    // So, the method is legit and should be considered.
                    // If the method is declared on an interface that has type parameters, we need to see how they are mapped to the type parameters of the interface

                    if (typeDeclaringMethod.getTypeParameters().isEmpty()) {
                        // No type parameters, we can add the method directly, but we need to check if it conflicts with an existing method
                        if (!isMethodAlreadyInList(resolvedDeclaration, methods)) {
                            methods.add(new VertxGenMethod(astDeclaration, method));
                        }
                    } else {
                        // We have type parameters, we need to see how they are mapped to our interface.
                        MethodUsage copy = method;
                        for (ResolvedTypeParameterDeclaration p : typeDeclaringMethod.getTypeParameters()) {
                            copy = copy.replaceTypeParameter(p,
                                    typeParameterMapping.get(typeDeclaringMethod.getQualifiedName() + "." + p.getName()));
                        }

                        // The method is updated, we need to see if it conflicts.
                        if (!isMethodAlreadyInList(copy, methods)) {
                            methods.add(new VertxGenMethod(astDeclaration, copy));
                        }
                    }
                }

                var fields = vg.getFields().stream()
                        .filter(f -> !AnnotationHelper.isIgnored(f))
                        .map(VertxGenConstant::new).toList();

                foundInterfaces.add(new VertxGenInterface(unit, vg, module, fqn, concrete, methods, fields, generator));
            }
        }
        logger.info("Found {} Vert.x Gen interfaces for module `{}`", foundInterfaces.size(), module.name());
        return foundInterfaces;
    }

    private boolean isMethodAlreadyInList(MethodUsage copy, List<VertxGenMethod> methods) {
        // First look at the method by name
        for (VertxGenMethod method : methods) {
            if (method.getName().equals(copy.getName())) {
                // Now check the parameters
                if (method.getParameters().size() == copy.getParamTypes().size()) {
                    boolean same = true;
                    for (int i = 0; i < method.getParameters().size(); i++) {
                        same = same && matches(copy.getParamTypes().get(i), method.getParameters().get(i).type());
                    }
                    if (same) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Looks for the `typeDeclaringMethod` into the extended interfaces of the `vg` interface.
     *
     * @param clz the extended types
     * @param typeDeclaringMethod the type declaring the method
     * @return the class or interface type representing the extended interface or {@code null} if not found
     */
    private ResolvedReferenceType lookForExtendedInterface(ClassOrInterfaceDeclaration clz,
            ResolvedReferenceTypeDeclaration typeDeclaringMethod) {
        String fqn = typeDeclaringMethod.getQualifiedName();
        for (ResolvedReferenceType type : clz.resolve().getAllAncestors()) {
            String fqn2 = type.asReferenceType().getQualifiedName();
            if (fqn.equals(fqn2)) {
                return type;
            }
        }

        return null;
    }

    private boolean isMethodAlreadyInList(ResolvedMethodDeclaration methodToBeAdded, List<VertxGenMethod> methods) {
        // First look at the method by name
        for (VertxGenMethod method : methods) {
            if (method.getName().equals(methodToBeAdded.getName())) {
                // Now check the parameters
                if (method.getParameters().size() == methodToBeAdded.getNumberOfParams()) {
                    boolean same = true;
                    for (int i = 0; i < method.getParameters().size(); i++) {
                        same = same && matches(methodToBeAdded.getParam(i).getType(), method.getParameters().get(i).type());
                    }
                    if (same) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean matches(ResolvedType typeFromNewMethod, ResolvedType typeFromExistingMethod) {
        if (typeFromNewMethod.isReferenceType() && typeFromExistingMethod.isReferenceType()) {
            return typeFromNewMethod.asReferenceType().getQualifiedName()
                    .equals(typeFromExistingMethod.asReferenceType().getQualifiedName());
        } else if (typeFromNewMethod.isTypeVariable()) {
            if (typeFromExistingMethod.isTypeVariable()) {
                return true;
            } else {
                return true; // Hackish but we would need to compute the type variable mapping
            }
        }
        return typeFromNewMethod.describe().equals(typeFromExistingMethod.describe());
    }

    private VertxGenClass lookupVertxGenClass(String fqn) {
        for (VertxGenClass itf : allInterfaces) {
            if (itf.fullyQualifiedName().equals(fqn)) {
                return itf;
            }
        }
        return null;
    }

    private boolean belongsToModule(String fqn, VertxGenModule module) {
        for (VertxGenClass itf : allInterfaces) {
            if (itf.fullyQualifiedName().equals(fqn) && itf.module().equals(module)) {
                return true;
            }
        }
        return false;
    }

    private VertxGenModule lookupModule(String moduleName) {
        if (moduleName == null) {
            if (modules.isEmpty()) {
                throw new IllegalArgumentException("No module name and no module found");
            } else if (modules.size() > 1) {
                throw new IllegalArgumentException(
                        "No module name, but multiple modules found: " + modules.stream().map(VertxGenModule::name).toList());
            }
            logger.info("No module name, using the (only) module found: {}", modules.getFirst().name());
            return modules.getFirst();
        }
        return modules.stream().filter(v -> v.name().equals(moduleName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No module found with the module name " + moduleName));
    }

    private List<VertxGenModule> collectVertxGenModuleFromCompilationUnits(List<CompilationUnit> cu) {
        List<VertxGenModule> collected = new ArrayList<>();
        for (CompilationUnit unit : cu) {
            var list = unit.findAll(PackageDeclaration.class).stream()
                    .filter(pd -> pd.isAnnotationPresent(ModuleGen.class))
                    .map(p -> {
                        AnnotationExpr expr = p.getAnnotationByClass(ModuleGen.class).orElseThrow();
                        String name = AnnotationHelper.getAttribute(expr, "name")
                                .map(a -> a.getValue().asStringLiteralExpr().getValue())
                                .orElseThrow();
                        String groupPackage = AnnotationHelper.getAttribute(expr, "groupPackage")
                                .map(a -> a.getValue().asStringLiteralExpr().getValue())
                                .orElseThrow();
                        return new VertxGenModule(name, groupPackage, List.of(p.getNameAsString()));
                    })
                    .toList();
            collected.addAll(list);
        }
        return collected;
    }

    private Set<String> collectDataObjectsFromCompilationUnits(List<CompilationUnit> units) {
        Set<String> dataObjects = new HashSet<>();
        for (CompilationUnit unit : units) {
            var list = unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(ClassOrInterfaceDeclaration::isClassOrInterfaceDeclaration)
                    .filter(c -> c.isAnnotationPresent(DataObject.class))
                    .map(c -> c.resolve().getQualifiedName())
                    .toList();
            dataObjects.addAll(list);
        }
        return dataObjects;
    }

    public List<VertxGenClass> collectVertxGenFromCompilationUnits(List<CompilationUnit> compilations) {
        List<VertxGenClass> itfs = new ArrayList<>();
        for (CompilationUnit unit : compilations) {
            var list = unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(ClassOrInterfaceDeclaration::isInterface)
                    .filter(c -> c.isAnnotationPresent(VertxGen.class))
                    .toList();
            for (ClassOrInterfaceDeclaration declaration : list) {
                String fqn = unit.getPackageDeclaration().map(p -> p.getName().asString() + ".").orElse("")
                        + declaration.getNameAsString();
                boolean concrete = AnnotationHelper
                        .getAttribute(declaration.getAnnotationByClass(VertxGen.class).orElseThrow(), CONCRETE_ATTRIBUTE)
                        .map(p -> p.getValue().asBooleanLiteralExpr().getValue()).orElse(true);
                // Need to identify the module
                VertxGenModule mod = lookupForModule(unit.getPackageDeclaration().get().getNameAsString());
                if (mod != null) {
                    itfs.add(new VertxGenClass(fqn, mod, concrete));
                } else {
                    logger.warn("No module found for `{}`", fqn);
                }
            }
        }
        return itfs;
    }

    /**
     * Find the Vert.x Gen modules closest to the given package name.
     *
     * @param packageName the package name
     * @return the module or {@code null} if no module is found
     */
    private VertxGenModule lookupForModule(String packageName) {
        VertxGenModule found = null;
        String closest = "";
        for (VertxGenModule mod : modules) {
            if (packageName.startsWith(mod.group())) {
                for (String pn : mod.packageNames()) {
                    if (packageName.startsWith(pn)) {
                        if (found == null || pn.length() > closest.length()) {
                            found = mod;
                            closest = pn;
                        }
                    }
                }
            }
        }
        return found;
    }

    private MethodDeclaration lookForMethodDeclaration(ResolvedReferenceTypeDeclaration type, MethodUsage method) {
        List<CompilationUnit> all = new ArrayList<>(units);
        all.addAll(additionalUnits);
        for (CompilationUnit unit : all) {
            ClassOrInterfaceDeclaration declaration = unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(ClassOrInterfaceDeclaration::isClassOrInterfaceDeclaration)
                    .filter(d -> d.resolve().getQualifiedName().equals(type.getQualifiedName()))
                    .findFirst().orElse(null);
            if (declaration != null) {
                // Look for method - only compare name and number of parameters
                for (MethodDeclaration md : declaration.getMethods()) {
                    if (md.getNameAsString().equals(method.getName())
                            && md.getParameters().size() == method.getParamTypes().size()) {

                        // Check parameters
                        for (int i = 0; i < md.getParameters().size(); i++) {
                            ResolvedType resolvedType = method.getParamTypes().get(i);
                            if (!resolvedType.isReferenceType()) {
                                logger.debug(
                                        "Ignoring method `{}` in interface `{}` as it has a non-reference type parameter",
                                        method.getName(), type.getQualifiedName());
                                break;
                            }
                            String name = resolvedType.asReferenceType().getQualifiedName();
                            if (!md.getParameter(i).getType().isReferenceType()) {
                                break;
                            }

                            String fqn = md.getParameter(i).getType().asReferenceType().asClassOrInterfaceType()
                                    .getNameWithScope();
                            if (!fqn.equals(name)) {
                                break;
                            }
                        }

                        return md;
                    }
                }
            }
        }
        return null;
    }
}
