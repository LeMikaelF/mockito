/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.ArrayEquals;
import org.mockito.internal.matchers.Equals;

/** by Szczepan Faber, created at: 3/31/12 */
public class ArgumentsProcessor implements Serializable {

    private final Object[] arguments;
    private final int parameterCount;
    private final boolean isVarArgs;

    public ArgumentsProcessor(Object[] arguments, int parameterCount, boolean isVarArgs) {
        this.arguments = arguments;
        this.parameterCount = parameterCount;
        this.isVarArgs = isVarArgs;
    }

    public Object[] toExpandedArgs() {
        {
            Object[] newArguments = arguments;
            if (arguments != null && arguments.length > parameterCount) {
                newArguments = Arrays.copyOf(arguments, parameterCount);
            } // drop extra arguments (currently -- Kotlin continuation synthetic
            // arg)
            return ArgumentsProcessor.expandVarArgs(isVarArgs, newArguments);
        }
    }

    // expands array varArgs that are given by runtime (1, [a, b]) into true
    // varArgs (1, a, b);
    private static Object[] expandVarArgs(final boolean isVarArgs, final Object[] args) {
        if (!isVarArgs
                || isNullOrEmpty(args)
                || (args[args.length - 1] != null && !args[args.length - 1].getClass().isArray())) {
            return args == null ? new Object[0] : args;
        }

        final int nonVarArgsCount = args.length - 1;
        Object[] varArgs;
        if (args[nonVarArgsCount] == null) {
            // in case someone deliberately passed null varArg array
            varArgs = new Object[] {null};
        } else {
            varArgs = ArrayEquals.createObjectArray(args[nonVarArgsCount]);
        }
        final int varArgsCount = varArgs.length;
        Object[] newArgs = new Object[nonVarArgsCount + varArgsCount];
        System.arraycopy(args, 0, newArgs, 0, nonVarArgsCount);
        System.arraycopy(varArgs, 0, newArgs, nonVarArgsCount, varArgsCount);
        return newArgs;
    }

    private static <T> boolean isNullOrEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static List<ArgumentMatcher> argumentsToMatchers(Object[] arguments) {
        List<ArgumentMatcher> matchers = new ArrayList<>(arguments.length);
        for (Object arg : arguments) {
            if (arg != null && arg.getClass().isArray()) {
                matchers.add(new ArrayEquals(arg));
            } else {
                matchers.add(new Equals(arg));
            }
        }
        return matchers;
    }

    public Object[] toContractedArgs() {
        if (!isVarArgs) {
            return arguments;
        }

        Object[] contractedArgs = new Object[parameterCount];
        int nonVarArgsCount = arguments.length - parameterCount;
        System.arraycopy(arguments, 0, contractedArgs, 0, nonVarArgsCount);
        Object[] onlyVarArgs = new Object[arguments.length - nonVarArgsCount];

        System.arraycopy(
                arguments, nonVarArgsCount, onlyVarArgs, 0, arguments.length - nonVarArgsCount);

        contractedArgs[contractedArgs.length - 1] = onlyVarArgs;
        return contractedArgs;
    }

    public Object[] getRawArguments() {
        return arguments;
    }
}
