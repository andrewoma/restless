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

package com.github.andrewoma.restless.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.andrewoma.restless.annotations.Service;
import com.github.andrewoma.restless.client.ClientExceptionHandler;
import com.github.andrewoma.restless.client.DefaultClientExceptionHandler;
import com.github.andrewoma.restless.core.Context;
import com.github.andrewoma.restless.core.DefaultContext;
import com.github.andrewoma.restless.core.proxy.MethodInterceptor;
import com.github.andrewoma.restless.core.proxy.ProxyFactory;
import com.github.andrewoma.restless.core.util.Validators;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HttpClientBuilder {
    private List<MethodInterceptor<Context>> interceptors = new ArrayList<MethodInterceptor<Context>>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private URI uri;
    private CloseableHttpClient httpClient;
    private ClientExceptionHandler exceptionHandler = new DefaultClientExceptionHandler();

    public HttpClientBuilder interceptor(MethodInterceptor<Context> interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    public HttpClientBuilder interceptors(Collection<MethodInterceptor<Context>> interceptors) {
        this.interceptors.addAll(interceptors);
        return this;
    }

    public HttpClientBuilder objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public HttpClientBuilder uri(URI uri) {
        this.uri = uri;
        return this;
    }

    public HttpClientBuilder uri(String uri) {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public HttpClientBuilder httpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public HttpClientBuilder exceptionHandler(ClientExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public <P> P build(Class<P> type) {
        Validators.require(type.isInterface(), "'type' must be an interface");

        final Service service = type.getAnnotation(Service.class);
        Validators.require(service != null, "'type' must have a 'Service' annotation");

        HttpClientMethodHandler<Context> handler = new HttpClientMethodHandler<Context>(httpClient, uri, objectMapper,
                exceptionHandler);

        return ProxyFactory.createProxy(type, interceptors, handler, new ProxyFactory.ContextFactory<Context>() {
            @Override
            public Context create() {
                return new DefaultContext(service.value());
            }
        });
    }
}
