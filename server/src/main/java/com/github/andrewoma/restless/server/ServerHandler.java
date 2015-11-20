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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.andrewoma.restless.core.ByteStream;
import com.github.andrewoma.restless.core.Context;
import com.github.andrewoma.restless.core.DefaultContext;
import com.github.andrewoma.restless.core.Headers;
import com.github.andrewoma.restless.core.Status;
import com.github.andrewoma.restless.core.Streamed;
import com.github.andrewoma.restless.core.StreamedType;
import com.github.andrewoma.restless.core.exception.NotFoundException;
import com.github.andrewoma.restless.core.proxy.MethodInterceptor;
import com.github.andrewoma.restless.core.proxy.MethodInvocation;
import com.github.andrewoma.restless.core.proxy.TargetMethodHandler;
import com.github.andrewoma.restless.core.util.Paranamers;
import com.github.andrewoma.restless.core.util.Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public class ServerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ServerHandler.class);

    private final ServiceProvider serviceProvider;
    private final List<MethodInterceptor<Context>> methodInterceptors;
    private final MappingJsonFactory jsonFactory;
    private final ObjectMapper objectMapper;
    private final ServerExceptionHandler exceptionHandler;

    public ServerHandler(ServiceProvider serviceProvider, List<MethodInterceptor<Context>> methodInterceptors,
            ObjectMapper objectMapper, ServerExceptionHandler exceptionHandler) {
        this.serviceProvider = serviceProvider;
        this.methodInterceptors = methodInterceptors;
        this.exceptionHandler = exceptionHandler;
        this.jsonFactory = new MappingJsonFactory(objectMapper);
        this.objectMapper = objectMapper;
    }

    public ServerResponse handleRequest(final ServerRequest request) {
        JsonParser requestParser = null;
        try {
            DefaultContext context = createContext(request);
            Service service = getService(request, context);
            Method method = getMethod(request, service.getType(), context);
            String[] parameterNames = Paranamers.DEFAULT.lookupParameterNames(method);

            Object[] parameters = getByteStreamParameters(method, request);
            if (parameters == null) {
                requestParser = jsonFactory.createParser(request.getRequest());
                parameters = parseParameters(requestParser, method.getParameterTypes(), parameterNames, method.getParameterAnnotations());
            }

            final Object result = new MethodInvocation<Context>(method, parameters, parameterNames, methodInterceptors,
                    new TargetMethodHandler<Context>(service.getInstance()), context).proceed();

            context.getResponseHeaders().put(Headers.STATUS.getValue(), Status.OK.getValue());

            return new ServerResponse(context.getResponseHeaders(), new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws Exception {
                    if (result instanceof ByteStream) {
                        ((ByteStream) result).output(output);
                    } else {
                        objectMapper.writeValue(output, result);
                    }
                }
            });
        } catch (Throwable t) {
            return handleException(t);
        } finally {
            DefaultContext.CONTEXTS.remove();
            close(requestParser);
        }
    }

    private ServerResponse handleException(Throwable throwable) {
        final MappedException exception = exceptionHandler.handle(throwable);
        return new ServerResponse(exception.getHeaders(), new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws Exception {
                objectMapper.writeValue(output, exception.getObject());
            }
        });
    }

    private Object[] getByteStreamParameters(Method method, final ServerRequest request) {
        if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(ByteStream.class)) {
            return new Object[]{new ByteStream() {
                @Override
                public InputStream input() {
                    return request.getRequest();
                }
            }};
        }
        return null;
    }

    private void close(JsonParser requestParser) {
        if (requestParser != null) {
            try {
                requestParser.close();
            } catch (Throwable t) {
                LOG.warn("Unable to close request parser", t);
            }
        }
    }

    private DefaultContext createContext(ServerRequest request) {
        DefaultContext context = new DefaultContext(request.getService());
        context.getRequestHeaders().putAll(request.getHeaders());
        DefaultContext.CONTEXTS.set(context);
        return context;
    }

    private Service getService(ServerRequest request, DefaultContext context) {
        Service service = serviceProvider.get(request.getService());
        if (service == null) {
            throw new NotFoundException(context.getRequestId(), "Unknown service '" + request.getService() + "'", null);
        }
        return service;
    }

    private Object[] parseParameters(JsonParser parser, Class<?>[] parameterTypes, String[] parameterNames,
            Annotation[][] annotations) throws IOException {

        // TODO ... support parameters in any order.
        Validators.require(parser.nextToken() == JsonToken.START_OBJECT, "Expect start of JSON object");
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            String name = parameterNames[i];
            String fieldName = parser.nextFieldName();
            Validators.require(name.equals(fieldName), "Expected '" + name + "' but got '" + fieldName + "'");
            Class<?> type = parameterTypes[i];
            parser.nextToken();
            if (type.equals(Streamed.class)) {
//                Class<?> streamedType = getStreamedType(annotations[i]);
            } else {
                parameters[i] = parser.readValueAs(type);
            }
        }
        Validators.require(parser.nextToken() == JsonToken.END_OBJECT, "Expect end of JSON object");
        return parameters;
    }

    private Class<?> getStreamedType(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(StreamedType.class)) {
                return ((StreamedType) annotation).value();
            }
        }
        throw new IllegalArgumentException("");
    }

    private Method getMethod(ServerRequest request, Class target, DefaultContext context) {
        for (Method method : target.getMethods()) {
            if (method.getName().equals(request.getMethod())) {
                return method;
            }
        }

        throw new NotFoundException(context.getRequestId(), "Method '" + request.getMethod() + "' not found on "
                + target.getClass().getCanonicalName(), null);
    }
}
