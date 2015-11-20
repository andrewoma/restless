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

package com.github.andrewoma.restless.example.server.security;

import com.github.andrewoma.restless.core.Context;
import com.github.andrewoma.restless.core.proxy.MethodInterceptor;
import com.github.andrewoma.restless.core.proxy.MethodInvocation;
import com.github.andrewoma.restless.core.proxy.TargetMethodHandler;
import com.github.andrewoma.restless.example.api.Headers;
import org.apache.shiro.authz.aop.AnnotationsAuthorizingMethodInterceptor;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import java.lang.reflect.Method;

public class SecurityInterceptor implements MethodInterceptor<Context> {
    private final org.apache.shiro.mgt.SecurityManager securityManager;

    public SecurityInterceptor(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public Object invoke(MethodInvocation<Context> invocation) throws Throwable {
        ThreadContext.bind(securityManager);

        String sessionId = invocation.getContext().getRequestHeaders().get(Headers.SESSION_ID);
        ThreadContext.bind(new Subject.Builder().sessionId(sessionId).buildSubject());

        return new AnnotationInterceptor().invoke(new InvocationAdapter(invocation));
    }

    public static class AnnotationInterceptor extends AnnotationsAuthorizingMethodInterceptor {
    }

    public static class InvocationAdapter implements org.apache.shiro.aop.MethodInvocation {
        private MethodInvocation<Context> invocation;

        public InvocationAdapter(MethodInvocation<Context> invocation) {
            this.invocation = invocation;
        }

        @Override
        public Object proceed() throws Throwable {
            return invocation.proceed();
        }

        @Override
        public Method getMethod() {
            return invocation.getMethod();
        }

        @Override
        public Object[] getArguments() {
            return invocation.getParameters();
        }

        @Override
        public Object getThis() {
            TargetMethodHandler<Context> handler = (TargetMethodHandler<Context>) invocation.getHandler();
            return handler.getTarget();
        }
    }
}
