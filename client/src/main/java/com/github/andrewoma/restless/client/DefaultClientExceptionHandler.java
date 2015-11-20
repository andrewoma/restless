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

package com.github.andrewoma.restless.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.andrewoma.restless.core.Headers;
import com.github.andrewoma.restless.core.exception.AuthenticationException;
import com.github.andrewoma.restless.core.exception.AuthorisationException;
import com.github.andrewoma.restless.core.exception.BadGatewayException;
import com.github.andrewoma.restless.core.exception.ClientException;
import com.github.andrewoma.restless.core.exception.ConcurrentModificationException;
import com.github.andrewoma.restless.core.exception.ConflictException;
import com.github.andrewoma.restless.core.exception.GatewayTimeoutException;
import com.github.andrewoma.restless.core.exception.NotFoundException;
import com.github.andrewoma.restless.core.exception.RemoteException;
import com.github.andrewoma.restless.core.exception.ServerException;
import com.github.andrewoma.restless.core.exception.ServiceUnavailableException;
import com.github.andrewoma.restless.core.exception.TooManyRequestsException;
import com.github.andrewoma.restless.core.exception.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultClientExceptionHandler implements ClientExceptionHandler {

    private Map<Integer, Class<? extends RemoteException>> exceptionTypes = new HashMap<Integer, Class<? extends RemoteException>>();

    @SuppressWarnings({"unchecked", "ThrowableResultOfMethodCallIgnored"})
    public DefaultClientExceptionHandler(RemoteException... custom) {
        List<RemoteException> exceptions = new ArrayList<RemoteException>(Arrays.asList(custom));
        exceptions.addAll(Arrays.asList(
                new AuthenticationException("", "", ""),
                new AuthorisationException("", "", ""),
                new BadGatewayException("", "", ""),
                new ClientException("", "", ""),
                new ConcurrentModificationException("", "", ""),
                new ConflictException("", "", ""),
                new GatewayTimeoutException("", "", ""),
                new NotFoundException("", "", ""),
                new ServerException("", "", ""),
                new ServiceUnavailableException("", "", ""),
                new TooManyRequestsException("", "", ""),
                new ValidationException("", "", "")
        ));

        for (RemoteException exception : exceptions) {
            exceptionTypes.put(exception.getStatus(), exception.getClass());
        }
    }

    @Override
    public Throwable handleException(Map<String, String> headers, ObjectMapper objectMapper, InputStream inputStream) throws IOException {
        int status = Integer.parseInt(headers.get(Headers.STATUS.getValue()));
        return objectMapper.readValue(inputStream, exceptionTypes.get(status));
    }
}
