/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.creation.bytebuddy;

import net.bytebuddy.implementation.bind.annotation.*;
import org.mockito.internal.debugging.LocationImpl;
import org.mockito.internal.invocation.ArgumentsProcessor;
import org.mockito.internal.invocation.RealMethod;
import org.mockito.internal.invocation.WithWeakReferenceHatch;
import org.mockito.invocation.Location;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;

import static org.mockito.internal.invocation.DefaultInvocationFactory.createInvocation;

public class MockMethodInterceptor implements Serializable {

    private static final long serialVersionUID = 7152947254057253027L;

    final MockHandler handler;

    private final MockCreationSettings mockCreationSettings;

    private final ByteBuddyCrossClassLoaderSerializationSupport serializationSupport;

    private transient ThreadLocal<Object> weakReferenceHatch = new ThreadLocal<>();

    public MockMethodInterceptor(MockHandler handler, MockCreationSettings mockCreationSettings) {
        this.handler = handler;
        this.mockCreationSettings = mockCreationSettings;
        serializationSupport = new ByteBuddyCrossClassLoaderSerializationSupport();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        weakReferenceHatch = new ThreadLocal<>();
    }

    Object doIntercept(
            Object mock,
            Method invokedMethod,
            ArgumentsProcessor argumentsProcessor,
            RealMethod realMethod)
            throws Throwable {
        return doIntercept(mock, invokedMethod, argumentsProcessor, realMethod, new LocationImpl());
    }

    Object doIntercept(
            Object mock,
            Method invokedMethod,
            ArgumentsProcessor argumentsProcessor,
            RealMethod realMethod,
            Location location)
            throws Throwable {
        return new WithWeakReferenceHatch<>(
                        mock,
                        weakReferenceHatch,
                        new WithWeakReferenceHatch.ThrowingSupplier<Object>() {
                            @Override
                            public Object get() throws Throwable {
                                return handler.handle(
                                        createInvocation(
                                                mock,
                                                invokedMethod,
                                                argumentsProcessor,
                                                realMethod,
                                                mockCreationSettings,
                                                location));
                            }
                        })
                .get();
    }

    public MockHandler getMockHandler() {
        return handler;
    }

    public ByteBuddyCrossClassLoaderSerializationSupport getSerializationSupport() {
        return serializationSupport;
    }

    public static final class ForHashCode {

        @SuppressWarnings("unused")
        public static int doIdentityHashCode(@This Object thiz) {
            return System.identityHashCode(thiz);
        }

        private ForHashCode() {}
    }

    public static class ForEquals {

        @SuppressWarnings("unused")
        public static boolean doIdentityEquals(@This Object thiz, @Argument(0) Object other) {
            return thiz == other;
        }
    }

    public static final class ForWriteReplace {

        public static Object doWriteReplace(@This MockAccess thiz) throws ObjectStreamException {
            return thiz.getMockitoInterceptor().getSerializationSupport().writeReplace(thiz);
        }

        private ForWriteReplace() {}
    }

    public static class DispatcherDefaultingToRealMethod {

        @SuppressWarnings("unused")
        @RuntimeType
        @BindingPriority(3)
        public static Object interceptSuperDefaultMethod(
                @This Object mock,
                @FieldValue("mockitoInterceptor") MockMethodInterceptor interceptor,
                @Origin Method invokedMethod,
                @AllArguments Object[] arguments,
                @Morph(serializableProxy = true, defaultMethod = true) Morphable morph)
                throws Throwable {
            return doInterceptSuper(mock, interceptor, invokedMethod, arguments, morph);
        }

        @SuppressWarnings("unused")
        @RuntimeType
        @BindingPriority(2)
        public static Object interceptSuper(
                @This Object mock,
                @FieldValue("mockitoInterceptor") MockMethodInterceptor interceptor,
                @Origin Method invokedMethod,
                @AllArguments Object[] arguments,
                @Morph(serializableProxy = true) Morphable morph)
                throws Throwable {
            return doInterceptSuper(mock, interceptor, invokedMethod, arguments, morph);
        }

        private static Object doInterceptSuper(
                @This Object mock,
                @FieldValue("mockitoInterceptor") MockMethodInterceptor interceptor,
                @Origin Method invokedMethod,
                @AllArguments Object[] arguments,
                @Morph(serializableProxy = true) Morphable morph)
                throws Throwable {
            if (interceptor == null) {
                return morph.call(arguments);
            }
            ArgumentsProcessor argumentsProcessor =
                    new ArgumentsProcessor(
                            arguments,
                            invokedMethod.getParameterTypes().length,
                            invokedMethod.isVarArgs());
            return interceptor.doIntercept(
                    mock,
                    invokedMethod,
                    argumentsProcessor,
                    new RealMethodWithArguments(morph, argumentsProcessor));
        }

        @SuppressWarnings("unused")
        @RuntimeType
        @BindingPriority(1)
        public static Object interceptAbstract(
                @This Object mock,
                @FieldValue("mockitoInterceptor") MockMethodInterceptor interceptor,
                @StubValue Object stubValue,
                @Origin Method invokedMethod,
                @AllArguments Object[] arguments)
                throws Throwable {
            if (interceptor == null) {
                return stubValue;
            }
            return interceptor.doIntercept(
                    mock,
                    invokedMethod,
                    new ArgumentsProcessor(
                            arguments,
                            invokedMethod.getParameterTypes().length,
                            invokedMethod.isVarArgs()),
                    RealMethod.IsIllegal.INSTANCE);
        }

        public interface Morphable {
            Object call(Object[] args);
        }
    }

    public static class RealMethodWithArguments implements RealMethod, Serializable {

        private final DispatcherDefaultingToRealMethod.Morphable morphable;
        private final ArgumentsProcessor argumentConverter;

        public RealMethodWithArguments(
                DispatcherDefaultingToRealMethod.Morphable morphable,
                ArgumentsProcessor argumentConverter) {
            this.morphable = morphable;
            this.argumentConverter = argumentConverter;
        }

        @Override
        public boolean isInvokable() {
            return true;
        }

        @Override
        public Object invoke() throws Throwable {
            return morphable.call(prepareExpandedArgumentsForMorphable());
        }

        private Object[] prepareExpandedArgumentsForMorphable() {
            return argumentConverter.toContractedArgs();
        }
    }
}
