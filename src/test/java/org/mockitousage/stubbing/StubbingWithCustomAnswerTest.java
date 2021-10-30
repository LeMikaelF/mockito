/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage.stubbing;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockitousage.IMethods;
import org.mockitousage.MethodsImpl;
import org.mockitoutil.TestBase;

public class StubbingWithCustomAnswerTest extends TestBase {
    @Mock private IMethods mock;

    @Test
    public void shouldAnswer() throws Exception {
        when(mock.simpleMethod(anyString()))
                .thenAnswer(
                        new Answer<String>() {
                            public String answer(InvocationOnMock invocation) throws Throwable {
                                String arg = invocation.getArgument(0);

                                return invocation.getMethod().getName() + "-" + arg;
                            }
                        });

        assertEquals("simpleMethod-test", mock.simpleMethod("test"));
    }

    @Test
    public void shouldAnswerWithThenAnswerAlias() throws Exception {
        RecordCall recordCall = new RecordCall();
        Set<?> mockedSet = (Set<?>) when(mock(Set.class).isEmpty()).then(recordCall).getMock();

        boolean unused = mockedSet.isEmpty();

        assertTrue(recordCall.isCalled());
    }

    @Test
    public void shouldAnswerConsecutively() throws Exception {
        when(mock.simpleMethod())
                .thenAnswer(
                        new Answer<String>() {
                            public String answer(InvocationOnMock invocation) throws Throwable {
                                return invocation.getMethod().getName();
                            }
                        })
                .thenReturn("Hello")
                .thenAnswer(
                        new Answer<String>() {
                            public String answer(InvocationOnMock invocation) throws Throwable {
                                return invocation.getMethod().getName() + "-1";
                            }
                        });

        assertEquals("simpleMethod", mock.simpleMethod());
        assertEquals("Hello", mock.simpleMethod());
        assertEquals("simpleMethod-1", mock.simpleMethod());
        assertEquals("simpleMethod-1", mock.simpleMethod());
    }

    @Test
    public void shouldAnswerVoidMethod() throws Exception {
        RecordCall recordCall = new RecordCall();

        doAnswer(recordCall).when(mock).voidMethod();

        mock.voidMethod();
        assertTrue(recordCall.isCalled());
    }

    @Test
    public void shouldAnswerVoidMethodConsecutively() throws Exception {
        RecordCall call1 = new RecordCall();
        RecordCall call2 = new RecordCall();

        doAnswer(call1)
                .doThrow(new UnsupportedOperationException())
                .doAnswer(call2)
                .when(mock)
                .voidMethod();

        mock.voidMethod();
        assertTrue(call1.isCalled());
        assertFalse(call2.isCalled());

        try {
            mock.voidMethod();
            fail();
        } catch (UnsupportedOperationException e) {
        }

        mock.voidMethod();
        assertTrue(call2.isCalled());
    }

    @Test
    public void shouldMakeSureTheInterfaceDoesNotChange() throws Exception {
        when(mock.simpleMethod(anyString()))
                .thenAnswer(
                        new Answer<String>() {
                            public String answer(InvocationOnMock invocation) throws Throwable {
                                assertTrue(invocation.getArguments().getClass().isArray());
                                assertEquals(Method.class, invocation.getMethod().getClass());

                                return "assertions passed";
                            }
                        });

        assertEquals("assertions passed", mock.simpleMethod("test"));
    }

    @Test
    public void
            given_spy_with_custom_answer__when_answer_reassigns_an_argument__then_real_method_is_called_with_assigned_argument() {
        //given
        MethodsImpl methodsImpl =
                new MethodsImpl() {
                    @Override
                    public String simpleMethod(String argument) {
                        return argument;
                    }
                };
        MethodsImpl spy = spy(methodsImpl);

        doAnswer(
                        invocation -> {
                            Object[] arguments = invocation.getArguments();
                            arguments[0] = "from answer";
                            return invocation.callRealMethod();
                        })
                .when(spy)
                .simpleMethod(anyString());

        //when
        String result = spy.simpleMethod("should be overwritten");

        //then
        assertEquals("from answer", result);
        verify(spy).simpleMethod("from answer");
    }

    @Test
    public void
    given_spy_with_custom_answer_and_varargs___when_answer_reassigns_arguments__then_real_method_is_called_with_assigned_arguments() {
        //given
        MethodsImpl methodsImpl =
            new MethodsImpl() {
                @Override
                public Object varargsObject(int i, Object... object) {
                    Object[] retVal = new Object[object.length + 1];
                    retVal[0] = i;
                    System.arraycopy(object, 0, retVal, 1, object.length);
                    return retVal;
                }
            };
        MethodsImpl spy = spy(methodsImpl);

        doAnswer(
            invocation -> {
                Object[] arguments = invocation.getArguments();
                arguments[0] = 1;
                arguments[1] = "from answer 1"; //FIXME ByteBuddy crée un array pour regrouper les varargs, donc ça ne marche pas.
                arguments[2] = 20;
                return invocation.callRealMethod();
            })
            .when(spy)
            .varargsObject(anyInt(), any());

        //when
        Object[] result = (Object[]) spy.varargsObject(0, "argument 1", 10);

        //then
        assertArrayEquals(new Object[]{1, "from answer 1", 20}, result);
        verify(spy).varargsObject(1, "from answer 1", 20);
    }

    private static class RecordCall implements Answer<Object> {
        private boolean called = false;

        public boolean isCalled() {
            return called;
        }

        public Object answer(InvocationOnMock invocation) throws Throwable {
            called = true;
            return null;
        }
    }
}
