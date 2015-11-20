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

package com.github.andrewoma.restless.server.http;

import com.github.andrewoma.restless.core.Headers;
import com.github.andrewoma.restless.core.util.CaseConverter;
import com.github.andrewoma.restless.core.util.Validators;
import com.github.andrewoma.restless.server.ServerHandler;
import com.github.andrewoma.restless.server.ServerRequest;
import com.github.andrewoma.restless.server.ServerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class RestlessServlet extends HttpServlet {
    public void setHandler(ServerHandler handler) {
        this.handler = handler;
    }

    private ServerHandler handler;

    public RestlessServlet(ServerHandler handler) {
        this.handler = handler;
    }

    public ServerHandler getHandler() {
        return handler;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String[] parts = request.getPathInfo().split("/");
        Validators.require(parts.length == 3, "Invalid path of '/<service>/<method>' but got '" + request.getPathInfo() + "'");
        String service = parts[1];
        String method = CaseConverter.lowerDashToLowerCamel(parts[2]);
        ServerRequest serverRequest = new ServerRequest(service, method, getHeaders(request), request.getInputStream());

        ServerResponse serverResponse = handler.handleRequest(serverRequest);

        handleResponse(response, serverResponse);
    }

    private void handleResponse(HttpServletResponse response, ServerResponse serverResponse) {
        response.setStatus(Integer.parseInt(serverResponse.getHeaders().get(Headers.STATUS.getValue())));

        for (Map.Entry<String, String> entry : serverResponse.getHeaders().entrySet()) {
            if (!entry.getKey().equals(Headers.STATUS.getValue())) {
                response.setHeader(entry.getKey(), entry.getValue());
            }
        }

        try {
            serverResponse.getOutput().write(response.getOutputStream());
        } catch (Exception e) {
            // TODO ... what to do here
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<String, String>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
