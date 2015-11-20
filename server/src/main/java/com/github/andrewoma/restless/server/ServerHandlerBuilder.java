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

package com.github.andrewoma.restless.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.andrewoma.restless.core.Context;
import com.github.andrewoma.restless.core.proxy.MethodInterceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerHandlerBuilder {
    private List<MethodInterceptor<Context>> interceptors = new ArrayList<MethodInterceptor<Context>>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private ServerExceptionHandler exceptionHandler = new DefaultServerExceptionHandler();
    private Map<String, Service> services = new HashMap<String, Service>();

    public ServerHandlerBuilder interceptor(MethodInterceptor<Context> interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    public ServerHandlerBuilder interceptors(Collection<MethodInterceptor<Context>> interceptors) {
        this.interceptors.addAll(interceptors);
        return this;
    }

    public ServerHandlerBuilder objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public ServerHandlerBuilder exceptionHandler(ServerExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public <T> ServerHandlerBuilder service(T instance) {
        for (Class<?> type : instance.getClass().getInterfaces()) {
            com.github.andrewoma.restless.annotations.Service service = type.getAnnotation(com.github.andrewoma.restless.annotations.Service.class);
            if (service != null) {
                services.put(service.value(), new Service(type, instance));
                return this;
            }
        }
        throw new IllegalArgumentException("A service instance must have an interface marked with a 'Service' annotation");
    }

    public ServerHandler build() {
        return new ServerHandler(new MapServiceProvider(services),
                interceptors, objectMapper, exceptionHandler);
    }
}
