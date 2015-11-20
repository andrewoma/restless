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

import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.AnnotationParanamer;
import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.CachingParanamer;
import com.thoughtworks.paranamer.DefaultParanamer;
import com.thoughtworks.paranamer.Paranamer;
import com.thoughtworks.paranamer.PositionalParanamer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class ProxyFactory {
    private static final Paranamer PARANAMER = new CachingParanamer(new AdaptiveParanamer(
            new AnnotationParanamer(),
            new BytecodeReadingParanamer(),
            new DefaultParanamer(),
            new PositionalParanamer())
    );
    public static final Object[] EMPTY_ARRAY = new Object[0];

    @SuppressWarnings("unchecked")
    public static <T, C> T createProxy(Class<T> anInterface, final List<MethodInterceptor<C>> interceptors,
            final MethodHandler<C> methodHandler, final ContextFactory<C> context) {

        // TODO ... support abstract classes
        return (T) Proxy.newProxyInstance(anInterface.getClassLoader(), new Class[]{anInterface}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return new MethodInvocation<C>(method, args == null ? EMPTY_ARRAY : args, PARANAMER.lookupParameterNames(method),
                        interceptors, methodHandler, context.create()).proceed();
            }
        });
    }

    public interface ContextFactory<T> {
        T create();
    }
}
