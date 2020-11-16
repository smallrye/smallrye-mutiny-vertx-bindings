package io.smallrye.mutiny.vertx.codegen.methods;

import io.vertx.codegen.MethodInfo;

import java.security.interfaces.DSAKey;

/**
 * Just a structure describing a method.
 */
public class MutinyMethodDescriptor {
    public final boolean fluent;
    private final MutinyKind kind;
    private final MethodInfo method;
    private final MethodInfo original;

    public static MutinyMethodDescriptor createAndForgetMethod(MethodInfo method, MethodInfo delegate, boolean fluent) {
        return new MutinyMethodDescriptor(method, delegate, MutinyKind.FORGET, fluent);
    }

    public MutinyMethodDescriptor(MethodInfo method, MethodInfo original, MutinyKind kind, boolean fluent) {
        this.kind = kind;
        this.fluent = fluent;
        this.method = method;
        this.original = original;
    }

    public MutinyMethodDescriptor(MethodInfo method, MethodInfo original, MutinyKind kind) {
        this(method, original, kind, false);
    }

    public String getMethodName() {
        return method.getName();
    }

    public String getOriginalMethodName() {
        return original.getName();
    }

    public MethodInfo getOriginalMethod() {
        return original;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public enum MutinyKind {
        UNI,
        AWAIT,
        FORGET,
        CONSUMER,
        SIMPLE //TODO  TO BE REMOVED
    }

    public boolean isFluent() {
        return fluent;
    }

    public boolean isDeprecated() {
        return method.isDeprecated();
    }

    public boolean isAwaitMethod() {
        return kind == MutinyKind.AWAIT;
    }

    public boolean isForgetMethod() {
        return kind == MutinyKind.FORGET;
    }

    public boolean isUniMethod() {
        return kind == MutinyKind.UNI;
    }

    public boolean isConsumerMethod() {
        return kind == MutinyKind.CONSUMER;
    }

    public boolean isSimple() {
        return kind == MutinyKind.SIMPLE;
    }


}
