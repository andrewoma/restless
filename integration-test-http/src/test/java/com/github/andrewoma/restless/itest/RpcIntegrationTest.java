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

package com.github.andrewoma.restless.itest;

import com.github.andrewoma.restless.client.DefaultClientExceptionHandler;
import com.github.andrewoma.restless.client.http.HttpClientMethodHandler;
import com.github.andrewoma.restless.core.ByteStream;
import com.github.andrewoma.restless.core.Context;
import com.github.andrewoma.restless.core.Contexts;
import com.github.andrewoma.restless.core.DefaultContext;
import com.github.andrewoma.restless.core.Streamed;
import com.github.andrewoma.restless.core.StreamedType;
import com.github.andrewoma.restless.core.exception.RemoteException;
import com.github.andrewoma.restless.core.proxy.MethodInterceptor;
import com.github.andrewoma.restless.core.proxy.MethodInvocation;
import com.github.andrewoma.restless.core.proxy.ProxyFactory;
import com.github.andrewoma.restless.server.DefaultServerExceptionHandler;
import com.github.andrewoma.restless.server.MapServiceProvider;
import com.github.andrewoma.restless.server.ServerHandler;
import com.github.andrewoma.restless.server.Service;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import org.junit.Test;

import javax.inject.Named;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RpcIntegrationTest extends AbstractIntegrationTest {
    private MethodInterceptor<Context> interceptor1 = new MethodInterceptor<Context>() {
        @Override
        public Object invoke(MethodInvocation<Context> invocation) throws Throwable {
            Context context = invocation.getContext();
            context.getRequestHeaders().put("header1", "value1");

            try {
                System.out.println("Before 1");
                return invocation.proceed();
            } finally {
                System.out.println("After 1");
            }
        }
    };

    private MethodInterceptor<Context> interceptor2 = new MethodInterceptor<Context>() {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            try {
                System.out.println("Before 2");
                return invocation.proceed();
            } finally {
                System.out.println("After 2");
            }
        }
    };

    @Test
    public void shouldHandleSuccessfulRequests() throws Exception {
        URI resolve = server.getURI().resolve("/rpc");
        HttpClientMethodHandler<Context> handler = new HttpClientMethodHandler<Context>(client, resolve, objectMapper,
                new DefaultClientExceptionHandler());

        Foo proxy = ProxyFactory.createProxy(Foo.class, ImmutableList.of(interceptor1, interceptor2), handler, new ProxyFactory.ContextFactory<Context>() {
            @Override
            public Context create() {
                return new DefaultContext("Foo");
            }
        });

        servlet.setHandler(new ServerHandler(new MapServiceProvider(ImmutableMap.of("Foo", new Service(Foo.class, new FooImpl()))),
                ImmutableList.<MethodInterceptor<Context>>of(), objectMapper, new DefaultServerExceptionHandler()));

        assertThat(proxy.bar("boo")).isEqualTo("value=boo");
        assertThat(proxy.baz("boo", "hoo")).isEqualTo("value1=boo,value2=hoo");
        FooResponse response = proxy.quk(FooRequest.create("boo", 1));
        assertThat(response.bar).isEqualTo(100);
        assertThat(response.foo).isEqualTo("foo=boo");
        proxy.noParams();
        try {
            proxy.throwsException();
        } catch (RemoteException e) {
            assertThat(e.getMessage()).isEqualTo("Broken");
        }

        ByteStream input = new ByteStream() {
            @Override
            public void output(OutputStream outputStream) throws IOException {
                outputStream.write("hello there server, this is the client".getBytes(Charsets.UTF_8));
            }
        };

        ByteStream output = proxy.stream(input);
        try {
            String hello = CharStreams.toString(new InputStreamReader(output.input(), Charsets.UTF_8));
            System.out.println("Client recieved: " + hello);
        } finally {
            output.close();
        }


//        Streamed<String> result = proxy.streaming(new Streamed<String>(String.class) {
//            @Override
//            public Iterator<String> iterator() {
//                return Arrays.asList("hello", "there").iterator();
//            }
//        });
//
//        for (String next : result) {
//            System.out.println(next);
//        }
    }

    public interface Foo {
        String bar(@Named("value") String value);

        String baz(@Named("value1") String value1, @Named("value2") String value2);

        FooResponse quk(@Named("request") FooRequest request);

        void noParams();

        void throwsException();

        Streamed<String> streaming(@StreamedType(String.class) @Named("request") Streamed<String> strings);

        ByteStream stream(ByteStream byteStream);
    }

    public static class FooRequest {
        public static FooRequest create(String foo, int bar) {
            FooRequest request = new FooRequest();
            request.foo = foo;
            request.bar = bar;
            return request;
        }

        public String foo;
        public int bar;
    }

    public static class FooResponse {
        public static FooResponse create(String foo, int bar) {
            FooResponse response = new FooResponse();
            response.foo = foo;
            response.bar = bar;
            return response;
        }

        public String foo;
        public int bar;
    }

    public static class FooImpl implements Foo {
        @Override
        public String bar(String value) {
            Context context = Contexts.get();
            System.out.println(context.getRequestHeaders());
            context.getResponseHeaders().put("server-header", "server-value");
            return "value=" + value;
        }

        @Override
        public String baz(String value1, String value2) {
            return "value1=" + value1 + ",value2=" + value2;
        }

        @Override
        public FooResponse quk(FooRequest request) {
            return FooResponse.create("foo=" + request.foo, request.bar * 100);
        }

        @Override
        public void noParams() {
        }

        @Override
        public void throwsException() {
            throw new IllegalArgumentException("Broken");
        }

        @Override
        public ByteStream stream(final ByteStream byteStream) {
            return new ByteStream() {
                @Override
                public void output(OutputStream output) throws Exception {
                    String hello = CharStreams.toString(new InputStreamReader(byteStream.input(), Charsets.UTF_8));
                    String message = "Server says: " + hello;

                    System.out.println(message);
                    output.write(message.getBytes(Charsets.UTF_8));
                }
            };
        }

        @Override
        public Streamed<String> streaming(Streamed<String> strings) {
            List<String> received = new ArrayList<String>();
            for (String value : strings) {
                received.add(value);
            }

            return null;
//
//            return new Streamed<String>(String.class) {
//                @Override
//                public Iterator<String> iterator() {
//                    return super.iterator();
//                }
//            };
        }
    }
}
