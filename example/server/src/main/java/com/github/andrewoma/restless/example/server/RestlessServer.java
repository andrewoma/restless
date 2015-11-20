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

import com.github.andrewoma.restless.server.ServerHandler;
import com.github.andrewoma.restless.server.http.LoggingFilter;
import com.github.andrewoma.restless.server.http.RestlessServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.DispatcherType;
import java.net.URI;
import java.util.EnumSet;

public class RestlessServer {
    private ServerHandler serverHandler;
    private String rootContext;

    public RestlessServer(ServerHandler serverHandler, String rootContext) {
        this.serverHandler = serverHandler;
        this.rootContext = rootContext;
    }

    private Server server;

    public void start() throws Exception {
        server = new Server(0);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        RestlessServlet servlet = new RestlessServlet(serverHandler);
        handler.addServletWithMapping(new ServletHolder(servlet), rootContext);
        handler.addFilterWithMapping(new FilterHolder(new LoggingFilter()), "/*", EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));
        server.start();
        System.out.println("Server available at " + server.getURI() + rootContext.replaceAll("^/", ""));
    }

    public void stop() throws Exception {
        server.stop();
    }

    public URI getURI() {
        return server.getURI();
    }
}
