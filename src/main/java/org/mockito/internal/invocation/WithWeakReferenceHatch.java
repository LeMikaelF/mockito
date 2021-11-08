/*
 * Copyright (c) 2021 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import java.io.Serializable;

// If the currently dispatched method is used in a hot path, typically a tight loop and if
// the mock is not used after the currently dispatched method, the JVM might attempt a
// garbage collection of the mock instance even before the execution of the current
// method is completed. Since we only reference the mock weakly from hereon after to avoid
// leaking the instance, it might therefore be garbage collected before the
// handler.handle(...) method completes. Since the handler method expects the mock to be
// present while a method call onto the mock is dispatched, this can lead to the problem
// described in GitHub #1802.
//
// To avoid this problem, we distract the JVM JIT by escaping the mock instance to a thread
// local field for the duration of the handler's dispatch.
//
// When dropping support for Java 8, instead of this hatch we should use an explicit fence
// https://docs.oracle.com/javase/9/docs/api/java/lang/ref/Reference.html#reachabilityFence-java.lang.Object-

public class WithWeakReferenceHatch<T> implements Serializable {

    private final Object mock;
    private final ThreadLocal<Object> weakReferenceHatch;
    private final ThrowingSupplier<T> throwingSupplier;

    public WithWeakReferenceHatch(
            Object mock, ThreadLocal<Object> weakReferenceHatch, ThrowingSupplier<T> supplier) {
        this.mock = mock;
        this.weakReferenceHatch = weakReferenceHatch;
        this.throwingSupplier = supplier;
    }

    public T get() throws Throwable {
        weakReferenceHatch.set(mock);
        try {
            return throwingSupplier.get();
        } finally {
            weakReferenceHatch.remove();
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> extends Serializable {
        T get() throws Throwable;
    }
}
