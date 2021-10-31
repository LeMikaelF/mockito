/*
 * Copyright (c) 2021 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.function.Function;

public interface RealMethodFactory extends Serializable {

    class FromMorphable implements RealMethodFactory {

        private final SerializableMorphable morphable;
        private int methodParamCount;
        private boolean methodIsVarArgs;

        public FromMorphable(
                MockMethodInterceptor.DispatcherDefaultingToRealMethod.Morphable morphable,
                int methodParamCount,
                boolean methodIsVarArgs) {
            this.morphable =
                    new SerializableMorphable() {
                        @Override
                        public Object apply(Object[] args1) {
                            return morphable.call(args1);
                        }
                    };
            this.methodParamCount = methodParamCount;
            this.methodIsVarArgs = methodIsVarArgs;
        }

        @Override
        public RealMethod create(Object[] args) {
            return new RealMethod.FromCallable(
                    new SerializableCallable() {
                        @Override
                        public Object call() throws Exception {
                            Object[] contractedArgs =
                                    ArgumentsProcessor.contractArgs(
                                            args, methodParamCount, methodIsVarArgs);
                            return morphable.apply(contractedArgs);
                        }
                    });
        }
    }

    RealMethod create(Object[] args);

    interface SerializableCallable extends Callable<Object>, Serializable {}

    interface SerializableMorphable
            extends Function<Object[], Object>, Serializable {} // TODO nécessaire?
}
