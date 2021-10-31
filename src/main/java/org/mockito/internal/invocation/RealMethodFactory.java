/*
 * Copyright (c) 2021 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor;

import java.io.Serializable;
import java.util.concurrent.Callable;

public interface RealMethodFactory {

    class FromMorphable implements RealMethodFactory {

        private final MockMethodInterceptor.DispatcherDefaultingToRealMethod.Morphable morphable;
        private final Object[] args;

        public FromMorphable(
                MockMethodInterceptor.DispatcherDefaultingToRealMethod.Morphable morphable,
                Object[] args) {
            this.morphable = morphable;
            this.args = args;
        }

        @Override
        public RealMethod create(Object[] args) {
            return new RealMethod.FromCallable(
                    new SerializableCallable() {
                        @Override
                        public Object call() throws Exception {
                            Object[] contractedArgs =
                                    ArgumentsProcessor.contractArgs(
                                            FromMorphable.this.args.length, args);
                            return morphable.call(contractedArgs);
                        }
                    });
        }
    }

    RealMethod create(Object[] args);

    interface SerializableCallable extends Callable<Object>, Serializable {}
}
