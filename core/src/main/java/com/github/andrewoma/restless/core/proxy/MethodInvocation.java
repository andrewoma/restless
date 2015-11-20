/*
 * Copyright (c) 2015 Andrew O'Malley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.andrewoma.restless.core.proxy;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

public class MethodInvocation<T> {
    private final Method method;
    private final Object[] parameters;
    private final String[] parameterNames;
    private final Iterator<MethodInterceptor<T>> interceptors;
    private final MethodHandler<T> handler;
    private final T context;
    private Object result;

    public MethodInvocation(Method method, Object[] parameters, String[] parameterNames, List<MethodInterceptor<T>> interceptors, MethodHandler<T> handler, T context) {
        this.method = method;
        this.parameters = parameters;
        this.parameterNames = parameterNames;
        this.interceptors = interceptors.iterator();
        this.handler = handler;
        this.context = context;
    }

    public Object proceed() throws Throwable {
        if (interceptors.hasNext()) {
            MethodInterceptor<T> next = interceptors.next();
            next.invoke(this);
        } else {
            result = handler.invoke(this);
        }
        return result;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public T getContext() {
        return context;
    }

    public MethodHandler<T> getHandler() {
        return handler;
    }
}
