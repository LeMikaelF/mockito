/*
 * Copyright (c) 2017 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.mockito.internal.creation.DelegatingMethod;
import org.mockito.internal.debugging.LocationImpl;
import org.mockito.internal.invocation.mockref.MockWeakReference;
import org.mockito.internal.progress.SequenceNumber;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.InvocationFactory;
import org.mockito.invocation.Location;
import org.mockito.mock.MockCreationSettings;

public class DefaultInvocationFactory implements InvocationFactory {

    public Invocation createInvocation(
            Object target,
            MockCreationSettings settings,
            Method method,
            final Callable realMethod,
            Object... args) {
        RealMethod superMethod = new RealMethod.FromCallable(realMethod);
        return createInvocation(
                target,
                settings,
                method,
                superMethod,
                new ArgumentsProcessor(args, method.getParameterCount(), method.isVarArgs()));
    }

    @Override
    public Invocation createInvocation(
            Object target,
            MockCreationSettings settings,
            Method method,
            RealMethodBehavior realMethod,
            Object... args) {
        RealMethod superMethod = new RealMethod.FromBehavior(realMethod);
        return createInvocation(
                target,
                settings,
                method,
                superMethod,
                new ArgumentsProcessor(args, method.getParameterCount(), method.isVarArgs()));
    }

    private Invocation createInvocation(
            Object target,
            MockCreationSettings settings,
            Method method,
            RealMethod superMethod,
            ArgumentsProcessor argumentsProcessor) {
        return createInvocation(
                target, method, argumentsProcessor, superMethod, settings, new LocationImpl());
    }

    public static InterceptedInvocation createInvocation(
            Object mock,
            Method invokedMethod,
            ArgumentsProcessor argumentsProcessor,
            RealMethod realMethod,
            MockCreationSettings settings,
            Location location) {
        return new InterceptedInvocation(
                new MockWeakReference<Object>(mock),
                createMockitoMethod(invokedMethod, settings),
                argumentsProcessor,
                realMethod,
                location,
                SequenceNumber.next());
    }

    private static MockitoMethod createMockitoMethod(Method method, MockCreationSettings settings) {
        if (settings.isSerializable()) {
            return new SerializableMethod(method);
        } else {
            return new DelegatingMethod(method);
        }
    }
}
