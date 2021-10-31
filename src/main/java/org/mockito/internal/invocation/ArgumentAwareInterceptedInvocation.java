/*
 * Copyright (c) 2021 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import org.mockito.internal.invocation.mockref.MockReference;
import org.mockito.invocation.Location;

public class ArgumentAwareInterceptedInvocation extends InterceptedInvocation {
    private ArgumentAwareInterceptedInvocation(
            MockReference<Object> mockRef,
            MockitoMethod mockitoMethod,
            Object[] expandedArgs,
            Object[] rawArguments,
            RealMethod realMethod,
            Location location,
            int sequenceNumber) {
        super(
                mockRef,
                mockitoMethod,
                expandedArgs,
                rawArguments,
                realMethod,
                sequenceNumber,
                location);
    }

    public static ArgumentAwareInterceptedInvocation of(
            MockReference<Object> mockRef,
            MockitoMethod mockitoMethod,
            Object[] arguments,
            RealMethodFactory realMethodFactory,
            Location location,
            int sequenceNumber) {
        Object[] expandedArgs = ArgumentsProcessor.expandArgs(mockitoMethod, arguments);
        RealMethod realMethod = realMethodFactory.create(expandedArgs);
        return new ArgumentAwareInterceptedInvocation(
                mockRef,
                mockitoMethod,
                expandedArgs,
                arguments,
                realMethod,
                location,
                sequenceNumber);
    }
}
