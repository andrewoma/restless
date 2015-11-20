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

package com.github.andrewoma.restless.example.client;

import com.github.andrewoma.restless.client.http.HttpClientBuilder;
import com.github.andrewoma.restless.example.api.AuthenticationService;
import com.github.andrewoma.restless.example.api.BlogService;
import com.github.andrewoma.restless.example.api.model.UserCredentials;
import com.github.andrewoma.restless.example.client.interceptor.AuthenticationInterceptor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;

public class BlogClientModule {
    private AuthenticationService authenticationService;
    private AuthenticationInterceptor authenticationInterceptor = new AuthenticationInterceptor();
    private CloseableHttpClient httpClient;

    public final BlogService blogs;

    public BlogClientModule(URI uri) {
        httpClient = HttpClients.createDefault();

        HttpClientBuilder builder = new HttpClientBuilder()
                .interceptor(authenticationInterceptor)
                .httpClient(httpClient)
                .uri(uri);

        authenticationService = builder.build(AuthenticationService.class);
        blogs = builder.build(BlogService.class);
    }

    public void login(UserCredentials userCredentials) {
        String sessionId = authenticationService.login(userCredentials);
        authenticationInterceptor.setSessionId(sessionId);
    }

    public void logout() {
        String sessionId = authenticationInterceptor.getSessionId();
        if (sessionId != null) {
            authenticationService.logout();
        }
    }

    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
