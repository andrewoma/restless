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

package com.github.andrewoma.restless.example.itest;

import com.github.andrewoma.restless.core.exception.AuthenticationException;
import com.github.andrewoma.restless.core.exception.AuthorisationException;
import com.github.andrewoma.restless.core.exception.ValidationException;
import com.github.andrewoma.restless.example.api.model.BlogDetails;
import com.github.andrewoma.restless.example.api.model.BlogResponse;
import com.github.andrewoma.restless.example.api.model.UserCredentials;
import com.github.andrewoma.restless.example.client.BlogClientModule;
import com.github.andrewoma.restless.example.server.BlogServerModule;
import com.github.andrewoma.restless.example.server.RestlessServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

import static org.junit.Assert.fail;

public class ClientServerIntegrationTest {
    private static BlogClientModule client;
    private static RestlessServer server;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void start() throws Exception {
        server = new RestlessServer(new BlogServerModule().getServerHandler(), "/*");
        server.start();
        client = new BlogClientModule(server.getURI());
    }

    @AfterClass
    public static void stop() throws Exception {
        client.close();
        server.stop();
    }

    @Before
    public void before() {
        System.out.println("\n=== " + name.getMethodName());
        client.logout();
    }

    @Test
    public void shouldThrowValidationErrorsOnInvalidInput() {
        client.login(new UserCredentials("andrewo", "password"));
        try {
            client.blogs.createBlog(null, null, new BlogDetails());
            fail();
        } catch (ValidationException e) {
            System.out.println(e.getFieldErrors());
        }
    }

    @Test
    public void shouldThrowAuthorisationExceptionIfUserHasNoPermission() {
        expectedException.expect(AuthorisationException.class);
        client.login(new UserCredentials("danielm", "password"));
        client.blogs.createBlog(null, null, new BlogDetails());
    }

    @Test
    public void shouldThrowAuthorisationExceptionIfNotLoggedIn() {
        expectedException.expect(AuthorisationException.class);
        client.blogs.createBlog(null, null, new BlogDetails());
    }

    @Test
    public void shouldThrowAuthenicationExceptionIfLoginUnsuccessful() {
        expectedException.expect(AuthenticationException.class);
        client.login(new UserCredentials("andrewo", "wrongpassword"));
    }
}
