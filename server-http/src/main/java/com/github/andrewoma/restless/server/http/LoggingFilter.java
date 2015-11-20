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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A stop-gap filter for logging requests and responses - this will be replaced with a LoggingInterceptor
 * that is aware of the structure of content (e.g. doesn't log streams, can hide fields such as passwords, etc).
 */
public class LoggingFilter implements Filter {
    private Set<String> EXCLUDED_HEADERS = new HashSet<String>(
            Arrays.asList("Transfer-Encoding", "Connection", "User-Agent", "Host", "Accept-Encoding", "Date", "Content-Type")
    );

    private static class ByteArrayServletStream extends ServletOutputStream {
        ByteArrayOutputStream baos;

        ByteArrayServletStream(ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        public void write(int param) throws IOException {
            baos.write(param);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }
    }

    private static class ByteArrayPrintWriter {

        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        private PrintWriter pw = new PrintWriter(baos);

        private ServletOutputStream sos = new ByteArrayServletStream(baos);

        public PrintWriter getWriter() {
            return pw;
        }

        public ServletOutputStream getStream() {
            return sos;
        }

        byte[] toByteArray() {
            return baos.toByteArray();
        }
    }

    private class BufferedServletInputStream extends ServletInputStream {

        ByteArrayInputStream bais;

        public BufferedServletInputStream(ByteArrayInputStream bais) {
            this.bais = bais;
        }

        public int available() {
            return bais.available();
        }

        public int read() {
            return bais.read();
        }

        public int read(byte[] buf, int off, int len) {
            return bais.read(buf, off, len);
        }

        @Override
        public boolean isFinished() {
            return bais.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
        }
    }

    private class BufferedRequestWrapper extends HttpServletRequestWrapper {

        ByteArrayInputStream bais;

        ByteArrayOutputStream baos;

        BufferedServletInputStream bsis;

        byte[] buffer;

        public BufferedRequestWrapper(HttpServletRequest req) throws IOException {
            super(req);
            InputStream is = req.getInputStream();
            baos = new ByteArrayOutputStream();
            byte buf[] = new byte[1024];
            int letti;
            while ((letti = is.read(buf)) > 0) {
                baos.write(buf, 0, letti);
            }
            buffer = baos.toByteArray();
        }

        public ServletInputStream getInputStream() {
            try {
                bais = new ByteArrayInputStream(buffer);
                bsis = new BufferedServletInputStream(bais);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return bsis;
        }

        public byte[] getBuffer() {
            return buffer;
        }

    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        BufferedRequestWrapper bufferedRequest = new BufferedRequestWrapper(httpRequest);

        StringBuilder sb = new StringBuilder();
        sb.append(">>> \n");
        sb.append(((HttpServletRequest) servletRequest).getMethod()).append(" ").append(((HttpServletRequest) servletRequest).getRequestURI()).append("\n");
        Enumeration<String> headerNames = ((HttpServletRequest) servletRequest).getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            if (!EXCLUDED_HEADERS.contains(header)) {
                String value = ((HttpServletRequest) servletRequest).getHeader(header);
                sb.append(header).append(": ").append(value).append("\n");
            }
        }

        sb.append(new String(bufferedRequest.getBuffer())).append("\n");

        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        final AtomicReference<Integer> status = new AtomicReference<Integer>(0);
        final ByteArrayPrintWriter pw = new ByteArrayPrintWriter();
        HttpServletResponse wrappedResp = new HttpServletResponseWrapper(response) {
            public PrintWriter getWriter() {
                return pw.getWriter();
            }

            public ServletOutputStream getOutputStream() {
                return pw.getStream();
            }

            @Override
            public void setStatus(int sc) {
                status.set(sc);
                super.setStatus(sc);
            }
        };

        filterChain.doFilter(bufferedRequest, wrappedResp);

        byte[] bytes = pw.toByteArray();
        response.getOutputStream().write(bytes);

        sb.append("<<<\n");
        sb.append("Status: ").append(status.get()).append("\n");

        for (String header : ((HttpServletResponse) servletResponse).getHeaderNames()) {
            if (!EXCLUDED_HEADERS.contains(header)) {
                String value = ((HttpServletRequest) servletRequest).getHeader(header);
                sb.append(header).append(": ").append(value).append("\n");
            }
        }

        sb.append(new String(bytes)).append("\n");
        sb.append("---");
        System.out.println(sb.toString());
    }

    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}
