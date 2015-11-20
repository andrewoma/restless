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

package com.github.andrewoma.restless.example.server;

import com.github.andrewoma.restless.core.Contexts;
import com.github.andrewoma.restless.core.exception.AuthenticationException;
import com.github.andrewoma.restless.core.exception.AuthorisationException;
import com.github.andrewoma.restless.example.api.AuthenticationService;
import com.github.andrewoma.restless.example.api.BlogService;
import com.github.andrewoma.restless.example.server.security.CustomRealm;
import com.github.andrewoma.restless.example.server.security.SecurityInterceptor;
import com.github.andrewoma.restless.example.server.security.ValidationInterceptor;
import com.github.andrewoma.restless.server.DefaultServerExceptionHandler;
import com.github.andrewoma.restless.server.ServerHandler;
import com.github.andrewoma.restless.server.ServerHandlerBuilder;
import org.apache.shiro.mgt.DefaultSecurityManager;

import javax.validation.Validation;
import javax.validation.Validator;

public class BlogServerModule {
    private ServerHandler serverHandler;
    private BlogService blogService;
    private AuthenticationService authenticationService;

    public BlogServerModule() {
        createServices();

        serverHandler = new ServerHandlerBuilder()
                .interceptor(new SecurityInterceptor(createSecurityManager()))
                .interceptor(new ValidationInterceptor(createValidator()))
                .service(authenticationService)
                .service(blogService)
                .exceptionHandler(createExceptionHandler())
                .build();
    }

    private DefaultServerExceptionHandler createExceptionHandler() {
        return new DefaultServerExceptionHandler() {
            @Override
            protected Throwable transform(Throwable t) {
                try {
                    throw t;
                } catch (org.apache.shiro.authc.AuthenticationException e) {
                    return new AuthenticationException(Contexts.get().getRequestId(), "Authentication failed", e.getMessage());
                } catch (org.apache.shiro.authz.AuthorizationException e) {
                    return new AuthorisationException(Contexts.get().getRequestId(), "Operation not permitted", e.getMessage());
                } catch (Throwable throwable) {
                    return t;
                }
            }
        };
    }

    private Validator createValidator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    private void createServices() {
        SecurityService securityService = new SecurityService();
        authenticationService = new AuthenticationServiceImpl();
        blogService = new BlogServiceImpl(securityService);
    }

    private DefaultSecurityManager createSecurityManager() {
        return new DefaultSecurityManager(new CustomRealm());
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }
}
