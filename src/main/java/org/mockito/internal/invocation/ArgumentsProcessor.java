/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.invocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.ArrayEquals;
import org.mockito.internal.matchers.Equals;

/** by Szczepan Faber, created at: 3/31/12 */
public final class ArgumentsProcessor {
    // drops hidden synthetic parameters (last continuation parameter from Kotlin suspending
    // functions)
    // and expands varargs
    public static Object[] expandArgs(MockitoMethod method, Object[] args) {
        int nParams = method.getParameterTypes().length;
        if (args != null && args.length > nParams) {
            args = Arrays.copyOf(args, nParams);
        } // drop extra args (currently -- Kotlin continuation synthetic
        // arg)
        return expandVarArgs(method.isVarArgs(), args);
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

    public static Object[] contractArgs(int nParams, Object[] expandedArgs) {
        if (expandedArgs == null || expandedArgs.length <= nParams) {
            return expandedArgs;
        }

        Object[] argsWithVarArgs = Arrays.copyOf(expandedArgs, nParams);

        int nonVarArgsCount = expandedArgs.length - nParams;
        Object[] varArgsArray = new Object[expandedArgs.length - nonVarArgsCount];

        System.arraycopy(
                expandedArgs,
                nonVarArgsCount,
                varArgsArray,
                0,
                expandedArgs.length - nonVarArgsCount);

        argsWithVarArgs[argsWithVarArgs.length - 1] = varArgsArray;

        return argsWithVarArgs;
    }
}
