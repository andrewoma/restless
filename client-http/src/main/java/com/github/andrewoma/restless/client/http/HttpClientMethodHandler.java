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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.andrewoma.restless.client.ClientExceptionHandler;
import com.github.andrewoma.restless.core.ByteStream;
import com.github.andrewoma.restless.core.ClosableByteStream;
import com.github.andrewoma.restless.core.Context;
import com.github.andrewoma.restless.core.Headers;
import com.github.andrewoma.restless.core.Streamed;
import com.github.andrewoma.restless.core.proxy.MethodHandler;
import com.github.andrewoma.restless.core.proxy.MethodInvocation;
import com.github.andrewoma.restless.core.util.CaseConverter;
import com.github.andrewoma.restless.core.util.Jdks;
import com.github.andrewoma.restless.core.util.Validators;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class HttpClientMethodHandler<T extends Context> implements MethodHandler<T> {
    private final CloseableHttpClient httpClient;
    private final URI baseUri;
    private final JsonFactory jsonFactory;
    private final ObjectMapper objectMapper;
    private final ClientExceptionHandler clientExceptionHandler;

    public HttpClientMethodHandler(CloseableHttpClient httpClient, URI baseUri, ObjectMapper objectMapper, ClientExceptionHandler clientExceptionHandler) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
        this.clientExceptionHandler = clientExceptionHandler;
        this.jsonFactory = new MappingJsonFactory(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public Object invoke(MethodInvocation<T> methodInvocation) throws Throwable {

        HttpPost request = createRequest(methodInvocation);

        CloseableHttpResponse response = httpClient.execute(request);

        try {
            Map<String, String> headers = getResponseHeaders(response);
            if (response.getStatusLine().getStatusCode() / 100 == 2) {
                return handleResponse(methodInvocation.getMethod().getReturnType(), response);
            } else {
                throw handleException(headers, response.getEntity().getContent());
            }
        } finally {
            if (!isByteStream(methodInvocation.getMethod().getReturnType())) {
                closeResponse(response);
            }
        }
    }

    private void closeResponse(CloseableHttpResponse response) throws IOException {
        try {
            EntityUtils.consume(response.getEntity());
        } finally {
            response.close();
        }
    }

    private boolean isByteStream(Class<?> returnType) {
        return returnType.isAssignableFrom(ByteStream.class);
    }

    private Throwable handleException(Map<String, String> headers, InputStream inputStream) throws IOException {
        return clientExceptionHandler.handleException(headers, objectMapper, inputStream);
    }

    private Object handleResponse(Class<?> returnType, final CloseableHttpResponse response) throws IOException {
        if (returnType.equals(Void.TYPE)) {
            return null;
        } else if (isByteStream(returnType)) {
            return createStreamingResponse(response);
        } else {
            return objectMapper.readValue(response.getEntity().getContent(), returnType);
        }
    }

    private Object createStreamingResponse(final CloseableHttpResponse response) {
        if (Jdks.supportsAutoCloseable()) {
            return new ClosableByteStream() {
                @Override
                public InputStream input() throws Exception {
                    return response.getEntity().getContent();
                }

                @Override
                public void close() throws Exception {
                    closeResponse(response);
                }
            };
        } else {
            return new ByteStream() {
                @Override
                public InputStream input() throws Exception {
                    return response.getEntity().getContent();
                }

                @Override
                public void close() throws Exception {
                    closeResponse(response);
                }
            };
        }
    }

    private HttpPost createRequest(final MethodInvocation<T> methodInvocation) throws IOException, URISyntaxException {
        URI uri = createRequestUri(methodInvocation);

        final HttpPost request = new HttpPost(uri);
        setRequestHeaders(request, methodInvocation.getContext().getRequestHeaders());
        request.setEntity(createEntity(methodInvocation));

        return request;
    }

    private StreamingEntity createEntity(final MethodInvocation<T> methodInvocation) {
        if (methodInvocation.getMethod().getParameterTypes().length == 1 &&
                methodInvocation.getMethod().getParameterTypes()[0].equals(ByteStream.class)) {

            return new StreamingEntity() {
                @Override
                public void writeTo(OutputStream outstream) throws IOException {
                    ByteStream parameter = (ByteStream) methodInvocation.getParameters()[0];
                    try {
                        parameter.output(outstream);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        StreamingEntity entity = new StreamingEntity() {
            @Override
            public void writeTo(OutputStream output) throws IOException {
                JsonGenerator generator = jsonFactory.createGenerator(output, JsonEncoding.UTF8);
                try {
                    generator.writeStartObject();

                    int numParams = methodInvocation.getParameters().length;

                    for (int i = 0; i < numParams; i++) {
                        String name = methodInvocation.getParameterNames()[i];
                        Object parameter = methodInvocation.getParameters()[i];
                        Class<?> type = methodInvocation.getMethod().getParameterTypes()[i];

                        if (type.equals(Streamed.class)) {
                            generator.writeArrayFieldStart(name);
                            for (Object value : (Streamed) parameter) {
                                generator.writeObject(value);
                            }
                            generator.writeEndArray();
                        } else {
                            Validators.require(!type.equals(ByteStream.class), "'ByteStream' parameters must be the first and only argument");
                            generator.writeObjectField(name, parameter);
                        }
                    }

                    generator.writeEndObject();
                } finally {
                    generator.close();
                }
            }
        };
        entity.setContentType(ContentType.APPLICATION_JSON.toString());
        return entity;
    }

    private URI createRequestUri(MethodInvocation<T> methodInvocation) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(baseUri);
        builder.setPath(builder.getPath() + (builder.getPath().endsWith("/") ? "" : "/")
                + methodInvocation.getContext().getServiceName()
                + "/" + CaseConverter.camelCaseToLowerDash(methodInvocation.getMethod().getName()));
        return builder.build();
    }

    private void setRequestHeaders(HttpPost request, Map<String, String> requestHeaders) {
        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, String> getResponseHeaders(HttpResponse response) {
        // TODO ... create a lazy view
        Header[] headers = response.getAllHeaders();
        Map<String, String> result = new HashMap<String, String>();
        for (Header header : headers) {
            result.put(header.getName(), header.getValue());
        }
        result.put(Headers.STATUS.getValue(), String.valueOf(response.getStatusLine().getStatusCode()));
        return result;
    }
}
