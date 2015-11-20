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

import com.github.andrewoma.restless.core.Contexts;
import com.github.andrewoma.restless.core.Headers;
import com.github.andrewoma.restless.core.Status;
import com.github.andrewoma.restless.core.exception.ClientException;
import com.github.andrewoma.restless.core.exception.RemoteException;
import com.github.andrewoma.restless.core.exception.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonMap;

public class DefaultServerExceptionHandler implements ServerExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultServerExceptionHandler.class);

    @Override
    public MappedException handle(Throwable t) {
        try {
            throw transform(t);
        } catch (ClientException e) {
            // Only log the stack trace for client exceptions if debug is enabled.
            if (LOG.isDebugEnabled()) {
                LOG.warn("code={} {}", e.getCode(), e.getMessage(), e);
            } else {
                LOG.warn("code={} {}", e.getCode(), e.getMessage());
            }
            return toMappedException(e);

        } catch (ServerException e) {
            LOG.error("code={}", e);

            return toMappedException(e);

        } catch (Throwable throwable) {
            LOG.error("Unhandled exception", t);
            return new MappedException(singletonMap(Headers.STATUS.getValue(), Status.ERROR.getValue()),
                    new ServerException(Contexts.get().getRequestId(), ServerException.DEFAULT_ERROR_CODE, t.getMessage(), null));
        }
    }

    private MappedException toMappedException(RemoteException e) {
        return new MappedException(singletonMap(Headers.STATUS.getValue(), String.valueOf(e.getStatus())), e);
    }

    protected Throwable transform(Throwable t) {
        return t;
    }
}
