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

package com.github.andrewoma.restless.core;

import com.github.andrewoma.restless.core.util.ThreadLocalRandom;

import javax.xml.bind.DatatypeConverter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DefaultContext implements Context {
    public static final ThreadLocal<DefaultContext> CONTEXTS = new ThreadLocal<DefaultContext>();

    public DefaultContext(String serviceName) {
        this.serviceName = serviceName;
    }

    private String serviceName;
    private String requestId;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private Map<String, String> responseHeaders = new HashMap<String, String>();
    private Map<String, Object> custom = Collections.emptyMap();

    public String getServiceName() {
        return serviceName;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) {
        return (T) custom.get(clazz.getName());
    }

    public <T> void set(Class<T> clazz, T value) {
        if (custom == Collections.EMPTY_MAP) {
            custom = new HashMap<String, Object>();
        }
        custom.put(clazz.getName(), value);
    }

    @Override
    public Object get(String key) {
        return custom.get(key);
    }

    @Override
    public void set(String key, Object value) {
        if (custom == Collections.EMPTY_MAP) {
            custom = new HashMap<String, Object>();
        }
        custom.put(key, value);
    }

    @Override
    public String getRequestId() {
        if (requestId == null) {
            requestId = requestHeaders.get(Headers.REQUEST_ID.getValue());
            if (requestId == null) {
                byte[] bytes = new byte[15];
                ThreadLocalRandom.current().nextBytes(bytes);
//                requestId = String.format("%x", new BigInteger(1, bytes));
                requestId = DatatypeConverter.printBase64Binary(bytes).replace('+', '-').replace('/', '_');
            }
        }
        return requestId;
    }

    public static void main(String[] args) {
        System.out.println(new DefaultContext("").getRequestId());
        System.out.println(UUID.randomUUID().toString());
    }
}
