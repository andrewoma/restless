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

package com.github.andrewoma.restless.example.readme;

import com.github.andrewoma.restless.client.http.HttpClientBuilder;
import com.github.andrewoma.restless.core.exception.NotFoundException;
import com.github.andrewoma.restless.core.exception.ValidationException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class ExampleServiceTest {
    private static ExampleServer server;
    private static ExampleService exampleService;
    private static CloseableHttpClient httpClient;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void start() throws Exception {
        server = new ExampleServer();
        server.start();
        httpClient = HttpClients.createDefault();
        exampleService = createClient(server.server.getURI());
    }

    private static ExampleService createClient(URI uri) {
        return new HttpClientBuilder()
                .httpClient(httpClient)
                .uri(uri)
                .build(ExampleService.class);
    }

    @AfterClass
    public static void stop() throws Exception {
        httpClient.close();
        server.server.stop();
    }

    @Test
    public void shouldEchoMessage() {
        String result = exampleService.echo("Andrew", "Hello");
        assertThat(result).isEqualTo("Hello Andrew! You said 'Hello'");
    }

    @Test
    public void shouldCreateAFoo() {
        long result = exampleService.createFoo(new Foo("myBar", 50));
        assertThat(result).isEqualTo(1);
        Foo foo = exampleService.getFoo(1L);
        assertThat(foo.bar).isEqualTo("myBar");
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundFindingNonExistingFoo() {
        exampleService.getFoo(666L);
    }

    @Test(expected = ValidationException.class)
    public void shouldRejectAnInvalidFoo() {
        exampleService.createFoo(new Foo(" ", 500));
    }
}
